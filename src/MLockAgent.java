/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

public final class MLockAgent {
  private MLockAgent() {}
  
  private static final String ARG_FORCE = "force";
  private static final String ARG_OPTIONAL = "optional";

  /**
   * Only the value of "MCL_CURRENT" is supported because...
   * <ul>
   *   <li>In most situations, you want the to create the JVM with a fixed 
   *       size heap (min==max), mlock those pages, and then do any 
   *       mmapping of data:<ul>
   *       <li>"The "right" solution is probably to use mlockall(MCL_CURRENT) 
   *           on JVM start (with min heap = max heap so that gets 
   *           pre-allocated). Then perform the mmapping."</li>
   *       <li>"Because we don't want mmap'd data to be locked into memory – 
   *           typical data sizes far exceed available RAM. The OS deals well 
   *           with keeping hot mmap'd data paged in, so we want to let it do 
   *           its job there. We just don't want it to be confused by the JVM's 
   *           GC behavior into paging part of the JVM itself out."</li>
   *     </ul>
   *   </li>
   *   <li>"...in practice, however, I can't find a single POSIX system that 
   *       assigns different values to MCL_CURRENT or MCL_FUTURE"</li>
   * </ul>
   *
   * @see https://issues.apache.org/jira/browse/CASSANDRA-1214
   */
  private static final int MCL_CURRENT = 1;
  //private static final int MCL_FUTURE = 2;

  private static native int mlockall(int flags) throws LastErrorException;

  public static void agentmain(String arg) {
    agent( arg );
  }
  public static void premain(String arg) {
    agent( arg );
  }
  public static void main(String[] args) {
    System.err.println("Demo mode of mlockall...");
    if (1 < args.length) {
      throw new IllegalArgumentException
        ("At most one command line arg may be specified");
    } 
    final String input = (0 == args.length) ? ARG_FORCE : args[0];
    try {
      agent( input );
      System.err.println("mlockall finished, sleeping so you can observe process info");
      while (true) {
        try {
          Thread.sleep(Long.MAX_VALUE); 
        } catch (InterruptedException e) {
          // :NOOP:
        }
      }
    } catch (RuntimeException e) {
      System.err.println("Demo mode of mlockall failed");
      throw e;
    }
  }

  /**
   * Invokes mlockall and checks the result status.  

   *
   * @param agentarg The arg string passed to this agent, may be "force" or 
   *                 "optional" to control what type of error checking is done
   * @exception UnsupportedOperationException if JNA or mlockall is not usable
   * @exception IllegalStateException if mlockall encounters an error
   * @exception IllegalArgumentException if agentarg is not recognized
   * @see #ARG_FORCE
   * @see #ARG_OPTIONAL
   */
  private static void agent(final String agentarg) {
    boolean force = true;
    if (ARG_FORCE.equals(agentarg) || "".equals(agentarg) || null == agentarg) {
      force = true;
    } else if (ARG_OPTIONAL.equals(agentarg)) {
      force = false;
    } else {
      throw new IllegalArgumentException
        ("Unable to parse agent arg, legal values are '"+ARG_FORCE+
         "' and '"+ARG_OPTIONAL+"'; arg used: " + agentarg);
    }

    try {
      try {
        Native.register("c");
      } catch (NoClassDefFoundError e) {
        throw new UnsupportedOperationException
          ("JNA not found. Unable to use mlockall()", e);
      } catch (UnsatisfiedLinkError e) {
        throw new UnsupportedOperationException
          ("Unable to link C library. Unable to use mlockall()", e);
      } catch (NoSuchMethodError e) {
        throw new UnsupportedOperationException
          ("Obsolete version of JNA present; unable to register C library; unable to use mlockall(). Upgrade to JNA 3.2.7 or later", e);
      }

      int result = -1;
      try {
        result = mlockall(MCL_CURRENT);
      } catch (UnsatisfiedLinkError e) {
        throw new UnsupportedOperationException
          ("Unable to link C library. Unable to use mlockall");
      } catch (RuntimeException e) {
        String error = "UNKNOWN";
        if (e instanceof LastErrorException) {
          error = String.valueOf(errno((LastErrorException) e));
        }
        throw new IllegalStateException
          ("Unable to lock JVM memory, error num: "+error);
      }
      if (0 != result) {
        throw new IllegalStateException
          ("Unexpected result from mlockall: " + result);
      }

    } catch (RuntimeException e) {
      if (force) throw e;
      return;
    }

  }

  private static int errno(LastErrorException e) {
    try {
      return e.getErrorCode();
    } catch (NoSuchMethodError x) {
      throw new UnsupportedOperationException("Obsolete version of JNA present; unable to read errno; unable to use mlockall(). Upgrade to JNA 3.2.7 or later");
    }
  }
}
