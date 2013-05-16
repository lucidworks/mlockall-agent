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

import java.util.logging.*;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

public final class MLockAgent {
  private MLockAgent() {}
  
  private static Logger logger = Logger.getLogger("MLockAgent");

  private static final int MCL_CURRENT = 1;
  private static final int MCL_FUTURE = 2;

  private static native int mlockall(int flags) throws LastErrorException;

  public static void agentmain(String arg) {
    doWork( arg );
  }
  public static void premain(String arg) {
    doWork( arg );
  }
  public static void main(String[] args) {
    logger.info("Demo mode of mlockall...");
    if ( doWork( null ) ) {
      logger.info("mlockall finished, sleeping so you can observe process info");
      while (true) {
        try {
          Thread.sleep(Long.MAX_VALUE); 
        } catch (InterruptedException e) {
          // :NOOP:
        }
      }
    } else {
      logger.info("Demo mode of mlockall failed");
    }
  }

  /**
   * @return true if JNA is supported and mlockall succeeded, otherwise false 
   *         (and details have already been logged)
   */
  private static boolean doWork(String arg) {
    // :TODO: parse arg for optional vs force, future vs current
    if (checkJna()) {
      return doMlockall();
    }
    return false;
  }

  /**
   * @return true if JNA seems to be supported on this platform, 
   *         otherwise false (and details have already been logged)
   */
  private static boolean checkJna() {
    try {
      Native.register("c");
      return true;
    } catch (NoClassDefFoundError e) {
      logger.severe("JNA not found. Unable to use mlockall()");
    } catch (UnsatisfiedLinkError e) {
      logger.severe("Unable to link C library. Unable to use mlockall()");
    } catch (NoSuchMethodError e) {
      logger.severe("Obsolete version of JNA present; unable to register C library; unable to use mlockall(). Upgrade to JNA 3.2.7 or later");
    }
    return false;
  }

  /**
   * @return true if mlockall succeeded, otherwise false 
   *         (and details have already been logged)
   */
  private static boolean doMlockall() {
    try {
      int result = mlockall(MCL_CURRENT);
      if (0 != result) {
        logger.warning("Unexpected result from mlockall: " + result);
        return false;
      }
    } catch (UnsatisfiedLinkError e) {
      // WTF? why didn't checkJna catch this?
      logger.severe("Unable to link C library. Unable to use mlockall()");
      return false;
    } catch (RuntimeException e) {
      String error = "UNKNOWN";
      if (e instanceof LastErrorException) {
        error = String.valueOf(errno((LastErrorException) e));
      }
      logger.severe("Unable to lock JVM memory ("+error+").");
      return false;
    }
    return true;
  }

  private static int errno(LastErrorException e) {
    try {
      return e.getErrorCode();
    } catch (NoSuchMethodError x) {
      logger.severe("Obsolete version of JNA present; unable to read errno; unable to use mlockall(). Upgrade to JNA 3.2.7 or later");
      return 0;
    }
  }
}
