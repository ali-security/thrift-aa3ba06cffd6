package org.apache.thrift.protocol;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.BitSet;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TSerializer;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;
import org.junit.jupiter.api.Test;
import thrift.test.ListDoublePerf;
import thrift.test.SingleMapTestStruct;
import thrift.test.TupleProtocolTestStruct;

public class TestTTupleProtocol extends ProtocolTestBase {

  @Override
  protected boolean canBeUsedNaked() {
    return false;
  }

  @Override
  protected TProtocolFactory getFactory() {
    return new TTupleProtocol.Factory();
  }

  @Test
  public void testBitsetLengthIssue() throws Exception {
    final TupleProtocolTestStruct t1 = new TupleProtocolTestStruct();
    t1.setField1(0);
    t1.setField2(12);
    new TDeserializer(new TTupleProtocol.Factory())
        .deserialize(
            new TupleProtocolTestStruct(),
            new TSerializer(new TTupleProtocol.Factory()).serialize(t1));
  }

  /**
   * The tuple scheme reads a container's element count straight off the wire, before any of its
   * elements. A peer can declare a count it never backs with data, so reading must not reserve
   * capacity for that count up front: the read has to fail once the data actually sent runs out.
   */
  @Override
  @Test
  public void testDeclaredContainerSizeIsNotPreallocated() throws Exception {
    // A large configured message size keeps checkReadBytesAvailable from rejecting the declared
    // count first, so the read reaches the container allocation itself.
    TConfiguration unlimited = TConfiguration.custom().setMaxMessageSize(Integer.MAX_VALUE).build();

    // SingleMapTestStruct: required map<i32,i32>, a bare count followed by its entries. A 1e9
    // count sizes a 2^30-bucket table for the first entry.
    TMemoryBuffer mapBuf = new TMemoryBuffer(unlimited, 0);
    TTupleProtocol mapOut = new TTupleProtocol(mapBuf);
    mapOut.writeI32(1_000_000_000);
    mapOut.writeI32(1);
    mapOut.writeI32(1);
    mapOut.writeI32(2);
    mapOut.writeI32(2);
    // The entries that were sent are read; the read then runs out of data.
    assertThrows(
        TTransportException.class,
        () -> new SingleMapTestStruct().read(new TTupleProtocol(mapBuf)),
        "Reading a map shorter than its declared size should fail");

    // ListDoublePerf: list<double>, preceded by the bitset of set fields. A 2e8 count would
    // preallocate a backing array larger than the whole test heap.
    TMemoryBuffer listBuf = new TMemoryBuffer(unlimited, 0);
    TTupleProtocol listOut = new TTupleProtocol(listBuf);
    BitSet present = new BitSet();
    present.set(0);
    listOut.writeBitSet(present, 1);
    listOut.writeI32(200_000_000);
    listOut.writeDouble(1.0);
    listOut.writeDouble(2.0);
    // The elements that were sent are read; the read then runs out of data.
    assertThrows(
        TTransportException.class,
        () -> new ListDoublePerf().read(new TTupleProtocol(listBuf)),
        "Reading a list shorter than its declared size should fail");
  }
}
