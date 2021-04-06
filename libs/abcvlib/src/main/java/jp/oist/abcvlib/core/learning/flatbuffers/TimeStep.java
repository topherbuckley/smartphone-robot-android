// automatically generated by the FlatBuffers compiler, do not modify

package jp.oist.abcvlib.core.learning.flatbuffers;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class TimeStep extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static TimeStep getRootAsTimeStep(ByteBuffer _bb) { return getRootAsTimeStep(_bb, new TimeStep()); }
  public static TimeStep getRootAsTimeStep(ByteBuffer _bb, TimeStep obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public TimeStep __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public jp.oist.abcvlib.core.learning.flatbuffers.WheelCounts wheelCounts() { return wheelCounts(new jp.oist.abcvlib.core.learning.flatbuffers.WheelCounts()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.WheelCounts wheelCounts(jp.oist.abcvlib.core.learning.flatbuffers.WheelCounts obj) { int o = __offset(4); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public jp.oist.abcvlib.core.learning.flatbuffers.ChargerData chargerData() { return chargerData(new jp.oist.abcvlib.core.learning.flatbuffers.ChargerData()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.ChargerData chargerData(jp.oist.abcvlib.core.learning.flatbuffers.ChargerData obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public jp.oist.abcvlib.core.learning.flatbuffers.BatteryData batteryData() { return batteryData(new jp.oist.abcvlib.core.learning.flatbuffers.BatteryData()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.BatteryData batteryData(jp.oist.abcvlib.core.learning.flatbuffers.BatteryData obj) { int o = __offset(8); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public jp.oist.abcvlib.core.learning.flatbuffers.ImageData imageData() { return imageData(new jp.oist.abcvlib.core.learning.flatbuffers.ImageData()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.ImageData imageData(jp.oist.abcvlib.core.learning.flatbuffers.ImageData obj) { int o = __offset(10); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public jp.oist.abcvlib.core.learning.flatbuffers.SoundData soundData() { return soundData(new jp.oist.abcvlib.core.learning.flatbuffers.SoundData()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.SoundData soundData(jp.oist.abcvlib.core.learning.flatbuffers.SoundData obj) { int o = __offset(12); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public jp.oist.abcvlib.core.learning.flatbuffers.RobotAction actions() { return actions(new jp.oist.abcvlib.core.learning.flatbuffers.RobotAction()); }
  public jp.oist.abcvlib.core.learning.flatbuffers.RobotAction actions(jp.oist.abcvlib.core.learning.flatbuffers.RobotAction obj) { int o = __offset(14); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }

  public static void startTimeStep(FlatBufferBuilder builder) { builder.startTable(6); }
  public static void addWheelCounts(FlatBufferBuilder builder, int wheelCountsOffset) { builder.addOffset(0, wheelCountsOffset, 0); }
  public static void addChargerData(FlatBufferBuilder builder, int chargerDataOffset) { builder.addOffset(1, chargerDataOffset, 0); }
  public static void addBatteryData(FlatBufferBuilder builder, int batteryDataOffset) { builder.addOffset(2, batteryDataOffset, 0); }
  public static void addImageData(FlatBufferBuilder builder, int imageDataOffset) { builder.addOffset(3, imageDataOffset, 0); }
  public static void addSoundData(FlatBufferBuilder builder, int soundDataOffset) { builder.addOffset(4, soundDataOffset, 0); }
  public static void addActions(FlatBufferBuilder builder, int actionsOffset) { builder.addStruct(5, actionsOffset, 0); }
  public static int endTimeStep(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public TimeStep get(int j) { return get(new TimeStep(), j); }
    public TimeStep get(TimeStep obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

