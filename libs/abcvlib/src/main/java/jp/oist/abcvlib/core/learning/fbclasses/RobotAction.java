// automatically generated by the FlatBuffers compiler, do not modify

package jp.oist.abcvlib.core.learning.fbclasses;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class RobotAction extends Struct {
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public RobotAction __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public byte motionAction() { return bb.get(bb_pos + 0); }
  public byte commAction() { return bb.get(bb_pos + 1); }

  public static int createRobotAction(FlatBufferBuilder builder, byte motionAction, byte commAction) {
    builder.prep(1, 2);
    builder.putByte(commAction);
    builder.putByte(motionAction);
    return builder.offset();
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public RobotAction get(int j) { return get(new RobotAction(), j); }
    public RobotAction get(RobotAction obj, int j) {  return obj.__assign(__element(j), bb); }
  }
}

