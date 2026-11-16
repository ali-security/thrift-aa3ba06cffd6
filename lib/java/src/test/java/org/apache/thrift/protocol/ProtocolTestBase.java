/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.thrift.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.thrift.Fixtures;
import org.apache.thrift.TBase;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.server.ServerTestBase;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.Test;
import thrift.test.CompactProtoTestStruct;
import thrift.test.Empty;
import thrift.test.HolyMoley;
import thrift.test.Nesting;
import thrift.test.OneOfEach;
import thrift.test.Srv;
import thrift.test.ThriftTest;

public abstract class ProtocolTestBase {

  /** Does it make sense to call methods like writeI32 directly on your protocol? */
  protected abstract boolean canBeUsedNaked();

  /** The protocol factory for the protocol being tested. */
  protected abstract TProtocolFactory getFactory();

  @Test
  public void testDouble() throws Exception {
    if (canBeUsedNaked()) {
      TMemoryBuffer buf = new TMemoryBuffer(1000);
      TProtocol proto = getFactory().getProtocol(buf);
      proto.writeDouble(123.456);
      assertEquals(123.456, proto.readDouble());
    }

    internalTestStructField(
        new StructFieldTestCase(TType.DOUBLE, (short) 15) {
          @Override
          public void readMethod(TProtocol proto) throws TException {
            assertEquals(123.456, proto.readDouble());
          }

          @Override
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeDouble(123.456);
          }
        });
  }

  @Test
  public void testSerialization() throws Exception {
    internalTestSerialization(OneOfEach.class, Fixtures.getOneOfEach());
    internalTestSerialization(Nesting.class, Fixtures.getNesting());
    internalTestSerialization(HolyMoley.class, Fixtures.getHolyMoley());
    internalTestSerialization(CompactProtoTestStruct.class, Fixtures.getCompactProtoTestStruct());
  }

  @Test
  public void testBinary() throws Exception {
    for (byte[] b :
        Arrays.asList(
            new byte[0],
            new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
            new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
            new byte[] {0x5D},
            new byte[] {(byte) 0xD5, (byte) 0x5D},
            new byte[] {(byte) 0xFF, (byte) 0xD5, (byte) 0x5D},
            new byte[128])) {
      if (canBeUsedNaked()) {
        internalTestNakedBinary(b);
      }
      internalTestBinaryField(b);
    }

    if (canBeUsedNaked()) {
      byte[] data = {1, 2, 3, 4, 5, 6};

      TMemoryBuffer buf = new TMemoryBuffer(0);
      TProtocol proto = getFactory().getProtocol(buf);
      ByteBuffer bb = ByteBuffer.wrap(data);
      bb.get();
      proto.writeBinary(bb.slice());
      assertEquals(ByteBuffer.wrap(data, 1, 5), proto.readBinary());
    }
  }

  @Test
  public void testString() throws Exception {
    for (String s :
        Arrays.asList("", "short", "borderlinetiny", "a bit longer than the smallest possible")) {
      if (canBeUsedNaked()) {
        internalTestNakedString(s);
      }
      internalTestStringField(s);
    }
  }

  @Test
  public void testUuid() throws Exception {
    UUID uuid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    if (canBeUsedNaked()) {
      internalTestNakedUuid(uuid);
    }
    internalTestUuidField(uuid);
  }

  @Test
  public void testLong() throws Exception {
    if (canBeUsedNaked()) {
      internalTestNakedI64(0);
    }
    internalTestI64Field(0);
    for (int i = 0; i < 62; i++) {
      if (canBeUsedNaked()) {
        internalTestNakedI64(1L << i);
        internalTestNakedI64(-(1L << i));
      }
      internalTestI64Field(1L << i);
      internalTestI64Field(-(1L << i));
    }
  }

  @Test
  public void testInt() throws Exception {
    for (int i :
        Arrays.asList(
            0, 1, 7, 150, 15000, 31337, 0xffff, 0xffffff, -1, -7, -150, -15000, -0xffff,
            -0xffffff)) {
      if (canBeUsedNaked()) {
        internalTestNakedI32(i);
      }
      internalTestI32Field(i);
    }
  }

  @Test
  public void testShort() throws Exception {
    for (int s : Arrays.asList(0, 1, 7, 150, 15000, 0x7fff, -1, -7, -150, -15000, -0x7fff)) {
      if (canBeUsedNaked()) {
        internalTestNakedI16((short) s);
      }
      internalTestI16Field((short) s);
    }
  }

  @Test
  public void testByte() throws Exception {
    if (canBeUsedNaked()) {
      internalTestNakedByte();
    }
    for (int i = 0; i < 128; i++) {
      internalTestByteField((byte) i);
      internalTestByteField((byte) -i);
    }
  }

  private void internalTestNakedByte() throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(1000);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeByte((byte) 123);
    assertEquals((byte) 123, proto.readByte());
  }

  private void internalTestByteField(final byte b) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.BYTE, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeByte(b);
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(b, proto.readByte());
          }
        });
  }

  private void internalTestNakedI16(short n) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeI16(n);
    assertEquals(n, proto.readI16());
  }

  private void internalTestI16Field(final short n) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.I16, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeI16(n);
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(n, proto.readI16());
          }
        });
  }

  private void internalTestNakedUuid(UUID uuid) throws TException {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol protocol = getFactory().getProtocol(buf);
    protocol.writeUuid(uuid);
    assertEquals(uuid, protocol.readUuid());
  }

  private void internalTestUuidField(UUID uuid) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.UUID, (short) 17) {
          @Override
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeUuid(uuid);
          }

          @Override
          public void readMethod(TProtocol proto) throws TException {
            assertEquals(uuid, proto.readUuid());
          }
        });
  }

  private void internalTestNakedI32(int n) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeI32(n);
    assertEquals(n, proto.readI32());
  }

  private void internalTestI32Field(final int n) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.I32, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeI32(n);
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(n, proto.readI32());
          }
        });
  }

  private void internalTestNakedI64(long n) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeI64(n);
    assertEquals(n, proto.readI64());
  }

  private void internalTestI64Field(final long n) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.I64, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeI64(n);
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(n, proto.readI64());
          }
        });
  }

  private void internalTestNakedString(String str) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeString(str);
    assertEquals(str, proto.readString());
  }

  private void internalTestStringField(final String str) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.STRING, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeString(str);
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(str, proto.readString());
          }
        });
  }

  private void internalTestNakedBinary(byte[] data) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);
    proto.writeBinary(ByteBuffer.wrap(data));
    assertEquals(ByteBuffer.wrap(data), proto.readBinary());
  }

  private void internalTestBinaryField(final byte[] data) throws Exception {
    internalTestStructField(
        new StructFieldTestCase(TType.STRING, (short) 15) {
          public void writeMethod(TProtocol proto) throws TException {
            proto.writeBinary(ByteBuffer.wrap(data));
          }

          public void readMethod(TProtocol proto) throws TException {
            assertEquals(ByteBuffer.wrap(data), proto.readBinary());
          }
        });
  }

  private <T extends TBase> void internalTestSerialization(Class<T> klass, T expected)
      throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TBinaryProtocol binproto = new TBinaryProtocol(buf);

    expected.write(binproto);

    buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);

    expected.write(proto);
    System.out.println("Size in " + proto.getClass().getSimpleName() + ": " + buf.length());

    T actual = klass.getDeclaredConstructor().newInstance();
    actual.read(proto);
    assertEquals(expected, actual);
  }

  @Test
  public void testMessage() throws Exception {
    List<TMessage> msgs =
        Arrays.asList(
            new TMessage[] {
              new TMessage("short message name", TMessageType.CALL, 0),
              new TMessage("1", TMessageType.REPLY, 12345),
              new TMessage(
                  "loooooooooooooooooooooooooooooooooong", TMessageType.EXCEPTION, 1 << 16),
              new TMessage("Janky", TMessageType.CALL, 0),
            });

    for (TMessage msg : msgs) {
      TMemoryBuffer buf = new TMemoryBuffer(0);
      TProtocol proto = getFactory().getProtocol(buf);
      TMessage output = null;

      proto.writeMessageBegin(msg);
      proto.writeMessageEnd();

      output = proto.readMessageBegin();

      assertEquals(msg, output);
    }
  }

  @Test
  public void testServerRequest() throws Exception {
    Srv.Iface handler =
        new Srv.Iface() {
          public int Janky(int i32arg) throws TException {
            return i32arg * 2;
          }

          public int primitiveMethod() throws TException {
            return 0;
          }

          public CompactProtoTestStruct structMethod() throws TException {
            return null;
          }

          public void voidMethod() throws TException {}

          public void methodWithDefaultArgs(int something) throws TException {}

          @Override
          public void onewayMethod() throws TException {}

          @Override
          public boolean declaredExceptionMethod(boolean shouldThrow) throws TException {
            return shouldThrow;
          }
        };

    Srv.Processor testProcessor = new Srv.Processor(handler);

    TMemoryBuffer clientOutTrans = new TMemoryBuffer(0);
    TProtocol clientOutProto = getFactory().getProtocol(clientOutTrans);
    TMemoryBuffer clientInTrans = new TMemoryBuffer(0);
    TProtocol clientInProto = getFactory().getProtocol(clientInTrans);

    Srv.Client testClient = new Srv.Client(clientInProto, clientOutProto);

    testClient.send_Janky(1);
    // System.out.println(clientOutTrans.inspect());
    testProcessor.process(clientOutProto, clientInProto);
    // System.out.println(clientInTrans.inspect());
    assertEquals(2, testClient.recv_Janky());
  }

  @Test
  public void testTDeserializer() throws TException {
    TSerializer ser = new TSerializer(getFactory());
    byte[] bytes = ser.serialize(Fixtures.getCompactProtoTestStruct());

    TDeserializer deser = new TDeserializer(getFactory());
    CompactProtoTestStruct cpts = new CompactProtoTestStruct();
    deser.deserialize(cpts, bytes);

    assertEquals(Fixtures.getCompactProtoTestStruct(), cpts);
  }

  /**
   * A container header is read before any of its elements, so a peer can declare an element count
   * that it never backs with data. Reading such a header must not reserve capacity for the declared
   * count up front: the read has to fail once the data actually sent runs out, not on an outsized
   * (or invalid) initial allocation. The tuple protocol carries no field headers, so
   * TestTTupleProtocol overrides this test.
   */
  @Test
  public void testDeclaredContainerSizeIsNotPreallocated() throws Exception {
    // A large configured message size keeps checkReadBytesAvailable from rejecting the declared
    // count first, so the read reaches the container allocation itself.
    TConfiguration unlimited = TConfiguration.custom().setMaxMessageSize(Integer.MAX_VALUE).build();

    // list<i8> byte_list: preallocating 1e9 elements needs a multi-gigabyte backing array.
    internalTestDeclaredContainerSize(
        unlimited,
        (short) 11,
        TType.LIST,
        proto -> {
          proto.writeListBegin(new TList(TType.BYTE, 1_000_000_000));
          proto.writeByte((byte) 1);
          proto.writeByte((byte) 2);
        });

    // set<i8> byte_set: doubling a 1.5e9 count for the hash capacity overflows int.
    internalTestDeclaredContainerSize(
        unlimited,
        (short) 20,
        TType.SET,
        proto -> {
          proto.writeSetBegin(new TSet(TType.BYTE, 1_500_000_000));
          proto.writeByte((byte) 1);
          proto.writeByte((byte) 2);
        });

    // map<i8,i8> byte_byte_map: a 1e9 count sizes a 2^30-bucket table for the first entry.
    internalTestDeclaredContainerSize(
        unlimited,
        (short) 29,
        TType.MAP,
        proto -> {
          proto.writeMapBegin(new TMap(TType.BYTE, TType.BYTE, 1_000_000_000));
          proto.writeByte((byte) 1);
          proto.writeByte((byte) 1);
          proto.writeByte((byte) 2);
          proto.writeByte((byte) 2);
        });

    // An empty struct has no minimum serialized size in some protocols, so even with the default
    // configuration a container of structs may declare any count at all.
    if (getFactory().getProtocol(new TMemoryBuffer(0)).getMinSerializedSize(TType.STRUCT) == 0) {
      TConfiguration defaults = new TConfiguration();

      // list<Empty> struct_list
      internalTestDeclaredContainerSize(
          defaults,
          (short) 19,
          TType.LIST,
          proto -> {
            proto.writeListBegin(new TList(TType.STRUCT, Integer.MAX_VALUE));
            new Empty().write(proto);
            new Empty().write(proto);
          });

      // set<Empty> struct_set
      internalTestDeclaredContainerSize(
          defaults,
          (short) 28,
          TType.SET,
          proto -> {
            proto.writeSetBegin(new TSet(TType.STRUCT, Integer.MAX_VALUE));
            new Empty().write(proto);
            new Empty().write(proto);
          });
    }
  }

  /**
   * The {@link TProtocol} collection read helpers size their default containers from the declared
   * element count too; they must not reserve capacity for a count that is never backed by data.
   */
  @Test
  public void testReadCollectionHelpersDoNotPreallocateDeclaredSize() throws Exception {
    if (!canBeUsedNaked()) {
      return;
    }
    TConfiguration unlimited = TConfiguration.custom().setMaxMessageSize(Integer.MAX_VALUE).build();

    TMemoryBuffer listBuf = new TMemoryBuffer(unlimited, 0);
    TProtocol listOut = getFactory().getProtocol(listBuf);
    listOut.writeListBegin(new TList(TType.BYTE, 1_000_000_000));
    listOut.writeByte((byte) 1);
    listOut.writeByte((byte) 2);
    TProtocol listIn = getFactory().getProtocol(listBuf);
    assertThrows(TTransportException.class, () -> listIn.readList(listIn::readByte));

    TMemoryBuffer setBuf = new TMemoryBuffer(unlimited, 0);
    TProtocol setOut = getFactory().getProtocol(setBuf);
    setOut.writeSetBegin(new TSet(TType.BYTE, 1_000_000_000));
    setOut.writeByte((byte) 1);
    setOut.writeByte((byte) 2);
    TProtocol setIn = getFactory().getProtocol(setBuf);
    assertThrows(TTransportException.class, () -> setIn.readSet(setIn::readByte));

    TMemoryBuffer mapBuf = new TMemoryBuffer(unlimited, 0);
    TProtocol mapOut = getFactory().getProtocol(mapBuf);
    mapOut.writeMapBegin(new TMap(TType.BYTE, TType.BYTE, 1_000_000_000));
    mapOut.writeByte((byte) 1);
    mapOut.writeByte((byte) 1);
    mapOut.writeByte((byte) 2);
    mapOut.writeByte((byte) 2);
    TProtocol mapIn = getFactory().getProtocol(mapBuf);
    assertThrows(
        TTransportException.class,
        () ->
            mapIn.readMap(
                new TProtocol.ReadMapEntryCallback<Byte, Byte>() {
                  @Override
                  public Byte getKey() throws TException {
                    return mapIn.readByte();
                  }

                  @Override
                  public Byte getValue() throws TException {
                    return mapIn.readByte();
                  }
                }));
  }

  //
  // Helper methods
  //

  private interface ContainerWriter {
    void write(TProtocol proto) throws TException;
  }

  /**
   * Writes the start of a CompactProtoTestStruct holding a single container field whose header
   * declares more elements than follow it, then reads it back.
   */
  private void internalTestDeclaredContainerSize(
      TConfiguration config, short fieldId, byte fieldType, ContainerWriter writer)
      throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(config, 0);
    TProtocol out = getFactory().getProtocol(buf);

    out.writeStructBegin(new TStruct("CompactProtoTestStruct"));
    out.writeFieldBegin(new TField("container", fieldType, fieldId));
    writer.write(out);
    // The data ends here, short of the element count the header declared.

    // Read through a fresh protocol: the writer above is left mid-struct, and a stateful protocol
    // (e.g. JSON) would otherwise read in the writer's nesting context.
    TProtocol in = getFactory().getProtocol(buf);
    // The elements that were sent are read; the read then runs out of data.
    assertThrows(
        TTransportException.class,
        () -> new CompactProtoTestStruct().read(in),
        "Reading a container shorter than its declared size should fail");
  }

  private void internalTestStructField(StructFieldTestCase testCase) throws Exception {
    TMemoryBuffer buf = new TMemoryBuffer(0);
    TProtocol proto = getFactory().getProtocol(buf);

    TField field = new TField("test_field", testCase.type_, testCase.id_);
    proto.writeStructBegin(new TStruct("test_struct"));
    proto.writeFieldBegin(field);
    testCase.writeMethod(proto);
    proto.writeFieldEnd();
    proto.writeStructEnd();

    proto.readStructBegin();
    TField readField = proto.readFieldBegin();
    assertEquals(testCase.id_, readField.id);
    assertEquals(testCase.type_, readField.type);
    testCase.readMethod(proto);
    proto.readStructEnd();
  }

  private abstract static class StructFieldTestCase {
    byte type_;
    short id_;

    public StructFieldTestCase(byte type, short id) {
      type_ = type;
      id_ = id;
    }

    public abstract void writeMethod(TProtocol proto) throws TException;

    public abstract void readMethod(TProtocol proto) throws TException;
  }

  private static final int NUM_TRIALS = 5;
  private static final int NUM_REPS = 10000;

  protected void benchmark() throws Exception {
    for (int trial = 0; trial < NUM_TRIALS; trial++) {
      TSerializer ser = new TSerializer(getFactory());
      byte[] serialized = null;
      long serStart = System.currentTimeMillis();
      for (int rep = 0; rep < NUM_REPS; rep++) {
        serialized = ser.serialize(Fixtures.getHolyMoley());
      }
      long serEnd = System.currentTimeMillis();
      long serElapsed = serEnd - serStart;
      System.out.println(
          "Ser:\t"
              + serElapsed
              + "ms\t"
              + ((double) serElapsed / NUM_REPS)
              + "ms per serialization");

      HolyMoley cpts = new HolyMoley();
      TDeserializer deser = new TDeserializer(getFactory());
      long deserStart = System.currentTimeMillis();
      for (int rep = 0; rep < NUM_REPS; rep++) {
        deser.deserialize(cpts, serialized);
      }
      long deserEnd = System.currentTimeMillis();
      long deserElapsed = deserEnd - deserStart;
      System.out.println(
          "Des:\t"
              + deserElapsed
              + "ms\t"
              + ((double) deserElapsed / NUM_REPS)
              + "ms per deserialization");
    }
  }

  private final ServerTestBase.TestHandler testHandler =
      new ServerTestBase.TestHandler() {
        @Override
        public String testString(String thing) {
          thing = thing + " Apache Thrift Java " + thing;
          return thing;
        }

        @Override
        public List<Integer> testList(List<Integer> thing) {
          thing.addAll(thing);
          thing.addAll(thing);
          return thing;
        }

        @Override
        public Set<Integer> testSet(Set<Integer> thing) {
          thing.addAll(thing.stream().map(x -> x + 100).collect(Collectors.toSet()));
          return thing;
        }

        @Override
        public Map<String, String> testStringMap(Map<String, String> thing) {
          thing.put("a", "123");
          thing.put(" x y ", " with spaces ");
          thing.put("same", "same");
          thing.put("0", "numeric key");
          thing.put("1", "");
          thing.put("ok", "2355555");
          thing.put("end", "0");
          return thing;
        }
      };

  private TProtocol initConfig(int maxSize) throws TException {
    TConfiguration config = TConfiguration.custom().setMaxMessageSize(maxSize).build();
    TMemoryBuffer bufferTrans = new TMemoryBuffer(config, 0);
    return getFactory().getProtocol(bufferTrans);
  }

  @Test
  public void testReadCheckMaxMessageRequestForString() throws TException {
    TProtocol clientOutProto = initConfig(15);
    TProtocol clientInProto = initConfig(15);
    ThriftTest.Client testClient = new ThriftTest.Client(clientInProto, clientOutProto);
    ThriftTest.Processor testProcessor = new ThriftTest.Processor(testHandler);
    try {
      testClient.send_testString("test");
      testProcessor.process(clientOutProto, clientInProto);
      String result = testClient.recv_testString();
      System.out.println("----result: " + result);
    } catch (TException e) {
      assertEquals(TTransportException.MESSAGE_SIZE_LIMIT, ((TTransportException) e).getType());
    }
  }

  @Test
  public void testReadCheckMaxMessageRequestForList() throws TException {
    TProtocol clientOutProto = initConfig(15);
    TProtocol clientInProto = initConfig(15);
    ThriftTest.Client testClient = new ThriftTest.Client(clientInProto, clientOutProto);
    ThriftTest.Processor testProcessor = new ThriftTest.Processor(testHandler);
    TTransportException e =
        assertThrows(
            TTransportException.class,
            () -> {
              testClient.send_testList(Arrays.asList(1, 23242346, 888888, 90));
              testProcessor.process(clientOutProto, clientInProto);
              testClient.recv_testList();
            },
            "Limitations not achieved as expected");
    assertEquals(TTransportException.MESSAGE_SIZE_LIMIT, e.getType());
  }

  @Test
  public void testReadCheckMaxMessageRequestForMap() throws TException {
    TProtocol clientOutProto = initConfig(13);
    TProtocol clientInProto = initConfig(13);
    ThriftTest.Client testClient = new ThriftTest.Client(clientInProto, clientOutProto);
    ThriftTest.Processor testProcessor = new ThriftTest.Processor(testHandler);
    Map<String, String> thing = new HashMap<>();
    thing.put("key", "Thrift");

    TTransportException e =
        assertThrows(
            TTransportException.class,
            () -> {
              testClient.send_testStringMap(thing);
              testProcessor.process(clientOutProto, clientInProto);
              testClient.recv_testStringMap();
            },
            "Limitations not achieved as expected");

    assertEquals(TTransportException.MESSAGE_SIZE_LIMIT, e.getType());
  }

  @Test
  public void testReadCheckMaxMessageRequestForSet() throws TException {
    TProtocol clientOutProto = initConfig(10);
    TProtocol clientInProto = initConfig(10);
    ThriftTest.Client testClient = new ThriftTest.Client(clientInProto, clientOutProto);
    ThriftTest.Processor testProcessor = new ThriftTest.Processor(testHandler);
    TTransportException e =
        assertThrows(
            TTransportException.class,
            () -> {
              testClient.send_testSet(
                  Stream.of(234, 0, 987087, 45, 88888888, 9).collect(Collectors.toSet()));
              testProcessor.process(clientOutProto, clientInProto);
              testClient.recv_testSet();
            },
            "Limitations not achieved as expected");
    assertEquals(TTransportException.MESSAGE_SIZE_LIMIT, e.getType());
  }
}
