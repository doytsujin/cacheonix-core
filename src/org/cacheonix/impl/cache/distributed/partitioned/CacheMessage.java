/*
 * Cacheonix Systems licenses this file to You under the LGPL 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.cacheonix.org/products/cacheonix/license-lgpl-2.1.htm
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cacheonix.impl.cache.distributed.partitioned;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.cacheonix.impl.net.processor.InvalidMessageException;
import org.cacheonix.impl.net.processor.Message;
import org.cacheonix.impl.net.processor.ProcessorKey;
import org.cacheonix.impl.net.serializer.SerializerUtils;
import org.cacheonix.impl.util.StringUtils;
import org.cacheonix.impl.util.logging.Logger;

/**
 * A message that is processed by a named cache.
 * <p/>
 *
 * @author <a href="mailto:simeshev@cacheonix.org">Slava Imeshev</a>
 * @since Jan 17, 2009 9:48:52 PM
 */
public abstract class CacheMessage extends Message {

   /**
    * Logger.
    *
    * @noinspection UNUSED_SYMBOL, UnusedDeclaration
    */
   private static final Logger LOG = Logger.getLogger(CacheMessage.class); // NOPMD

   /**
    * Cache name.
    */
   private String cacheName = null;


   /**
    * Constructor.
    *
    * @param wireableType a request type. See {@link Message} for cache message types.
    * @param cacheName    cache name.
    */
   CacheMessage(final int wireableType, final String cacheName) {

      super(validateMessageType(wireableType));
      this.cacheName = cacheName;
   }


   CacheMessage() {

   }


   /**
    * {@inheritDoc}
    */
   protected final ProcessorKey getProcessorKey() {

      return new CacheProcessorKey(cacheName);
   }


   /**
    * Returns partition name.
    *
    * @return partition name.
    */
   public final String getCacheName() {

      return cacheName;
   }


   /**
    * Returns context cache processor. The context cache processor is set as a part of the CacheMessage lifecycle in
    * {@link #execute()}.
    *
    * @return context cache processor.
    */
   final CacheProcessor getCacheProcessor() {

      return (CacheProcessor) getProcessor();
   }


   public void validate() throws InvalidMessageException {

      super.validate();
      if (StringUtils.isBlank(cacheName)) {
         throw new InvalidMessageException("Cache name is not set");
      }
   }


   /**
    * {@inheritDoc}
    * <p/>
    * Checks if the target cache processor exists. If not, posts "try-later" to the requester and exits. Otherwise
    * runs.
    */
   public final void execute() {

      final int state = getCacheProcessor().getState();
      switch (state) {
         case CacheProcessor.STATE_BLOCKED:
            executeBlocked();
            break;
         case CacheProcessor.STATE_OPERATIONAL:
            executeOperational();
            break;
         default:
            executeUnknown(state);
            break;
      }
   }


   /**
    * Executes the messages at the server while the cache processor is in operational state.
    */
   protected abstract void executeOperational();


   /**
    * Executes the messages at the server while the cache processor is in blocked state.
    */
   protected abstract void executeBlocked();


   /**
    * Handles erroneous situation when the cache processor is an unknown state.
    *
    * @param state the problem state.
    */
   private static void executeUnknown(final int state) {

      LOG.warn("Cache processor is in an unknown state: " + state);
   }


   /**
    * {@inheritDoc}
    */
   public void readWire(final DataInputStream in) throws IOException, ClassNotFoundException {

      super.readWire(in);
      cacheName = SerializerUtils.readString(in);
   }


   /**
    * {@inheritDoc}
    */
   public void writeWire(final DataOutputStream out) throws IOException {

      super.writeWire(out);
      SerializerUtils.writeString(cacheName, out);
   }


   /**
    * Ensures that inheritors are using a correct base message destination.
    *
    * @param type message type to validate, must have DESTINATION_CACHE.
    * @return type same as parameter if it is valid.
    */
   private static int validateMessageType(final int type) {

      final int destination = convertTypeToDestination(type);
      final int expected = DESTINATION_CACHE_PROCESSOR;
      if (destination == expected) {
         return type;
      }

      throw new IllegalArgumentException("Expected destination type " + expected + " but it was " + destination);
   }


   public String toString() {

      return "CacheMessage{" +
              "cacheName='" + cacheName + '\'' +
              "} " + super.toString();
   }
}