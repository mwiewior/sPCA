/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qcri.pca;

import static org.easymock.EasyMock.createMockBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.MapContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.ReduceContext;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.StatusReporter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.util.Progress;

/**
 * The same as {@link org.apache.mahout.common.DummyRecordWriter} except that
 * the write method is fixed
 * 
 * @param <K>
 * @param <V>
 * 
 * @author maysam yabandeh
 */
public final class DummyRecordWriter<K extends Writable, V extends Writable>
    extends RecordWriter<K, V> {

  private final Map<K, List<V>> data = new TreeMap<K, List<V>>();

  private void cloneWritable(Writable from, Writable to) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    from.write(dos);
    dos.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    DataInputStream dis = new DataInputStream(bais);
    to.readFields(dis);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(K key, V value) {
    // if the user reuses the same writable class, we need to create a new one
    // otherwise the Map content will be modified after the insert
    try {
      if (!(key instanceof NullWritable)) {
        K newKey = (K) key.getClass().newInstance();
        cloneWritable(key, newKey);
        key = newKey;
      }
      V newValue = (V) value.getClass().newInstance();
      cloneWritable(value, newValue);
      value = newValue;
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<V> points = data.get(key);
    if (points == null) {
      points = Lists.newArrayList();
      data.put(key, points);
    }
    points.add(value);
  }

  @Override
  public void close(TaskAttemptContext context) {
  }

  public Map<K, List<V>> getData() {
    return data;
  }

  public List<V> getValue(K key) {
    return data.get(key);
  }

  public Set<K> getKeys() {
    return data.keySet();
  }

  public static <K1, V1, K2, V2> Mapper<K1, V1, K2, V2>.Context build(
      Mapper<K1, V1, K2, V2> mapper, Configuration configuration,
      RecordWriter<K2, V2> output) throws IOException, InterruptedException {

    // Use reflection since the context types changed incompatibly between 0.20
    // and 0.23.
    try {
      return buildNewMapperContext(configuration, output);
    } catch (Exception e) {
      try {
        return buildOldMapperContext(mapper, configuration, output);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  public static <K1, V1, K2, V2> Reducer<K1, V1, K2, V2>.Context build(
      Reducer<K1, V1, K2, V2> reducer, Configuration configuration,
      RecordWriter<K2, V2> output, Class<K1> keyClass, Class<V1> valueClass)
      throws IOException, InterruptedException {

    // Use reflection since the context types changed incompatibly between 0.20
    // and 0.23.
    try {
      return buildNewReducerContext(configuration, output, keyClass, valueClass);
    } catch (Exception e) {
      try {
        return buildOldReducerContext(reducer, configuration, output, keyClass,
            valueClass);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <K1, V1, K2, V2> Mapper<K1, V1, K2, V2>.Context buildNewMapperContext(
      Configuration configuration, RecordWriter<K2, V2> output)
      throws Exception {
    Class<?> mapContextImplClass = Class
        .forName("org.apache.hadoop.mapreduce.task.MapContextImpl");
    Constructor<?> cons = mapContextImplClass.getConstructors()[0];
    Object mapContextImpl = cons.newInstance(configuration,
        new TaskAttemptID(), null, output, null, new DummyStatusReporter(),
        null);

    Class<?> wrappedMapperClass = Class
        .forName("org.apache.hadoop.mapreduce.lib.map.WrappedMapper");
    Object wrappedMapper = wrappedMapperClass.newInstance();
    Method getMapContext = wrappedMapperClass.getMethod("getMapContext",
        MapContext.class);
    return (Mapper.Context) getMapContext.invoke(wrappedMapper, mapContextImpl);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <K1, V1, K2, V2> Mapper<K1, V1, K2, V2>.Context buildOldMapperContext(
      Mapper<K1, V1, K2, V2> mapper, Configuration configuration,
      RecordWriter<K2, V2> output) throws Exception {
    Constructor<?> cons = getNestedContextConstructor(mapper.getClass());
    // first argument to the constructor is the enclosing instance
    return (Mapper.Context) cons.newInstance(mapper, configuration,
        new TaskAttemptID(), null, output, null, new DummyStatusReporter(),
        null);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <K1, V1, K2, V2> Reducer<K1, V1, K2, V2>.Context buildNewReducerContext(
      Configuration configuration, RecordWriter<K2, V2> output,
      Class<K1> keyClass, Class<V1> valueClass) throws Exception {
    Class<?> reduceContextImplClass = Class
        .forName("org.apache.hadoop.mapreduce.task.ReduceContextImpl");
    Constructor<?> cons = reduceContextImplClass.getConstructors()[0];
    Object reduceContextImpl = cons.newInstance(configuration,
        new TaskAttemptID(), new MockIterator(), null, null, output, null,
        new DummyStatusReporter(), null, keyClass, valueClass);

    Class<?> wrappedReducerClass = Class
        .forName("org.apache.hadoop.mapreduce.lib.reduce.WrappedReducer");
    Object wrappedReducer = wrappedReducerClass.newInstance();
    Method getReducerContext = wrappedReducerClass.getMethod(
        "getReducerContext", ReduceContext.class);
    return (Reducer.Context) getReducerContext.invoke(wrappedReducer,
        reduceContextImpl);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <K1, V1, K2, V2> Reducer<K1, V1, K2, V2>.Context buildOldReducerContext(
      Reducer<K1, V1, K2, V2> reducer, Configuration configuration,
      RecordWriter<K2, V2> output, Class<K1> keyClass, Class<V1> valueClass)
      throws Exception {
    Constructor<?> cons = getNestedContextConstructor(reducer.getClass());
    // first argument to the constructor is the enclosing instance
    return (Reducer.Context) cons.newInstance(reducer, configuration,
        new TaskAttemptID(), new MockIterator(), null, null, output, null,
        new DummyStatusReporter(), null, keyClass, valueClass);
  }

  private static Constructor<?> getNestedContextConstructor(Class<?> outerClass) {
    for (Class<?> nestedClass : outerClass.getClasses()) {
      if ("Context".equals(nestedClass.getSimpleName())) {
        return nestedClass.getConstructors()[0];
      }
    }
    throw new IllegalStateException("Cannot find context class for "
        + outerClass);
  }

  static public final class MockIterator implements RawKeyValueIterator {

    @Override
    public void close() {
    }

    @Override
    public DataInputBuffer getKey() {
      return null;
    }

    @Override
    public Progress getProgress() {
      return null;
    }

    @Override
    public DataInputBuffer getValue() {

      return null;
    }

    @Override
    public boolean next() {
      return true;
    }

  }
  
  static public final class DummyStatusReporter extends StatusReporter {

    private final Map<Enum<?>, Counter> counters = Maps.newHashMap();
    private final Map<String, Counter> counterGroups = Maps.newHashMap();

    private Counter newCounter() {
      try {
        // 0.23 case
        String c = "org.apache.hadoop.mapreduce.counters.GenericCounter";
        return (Counter) createMockBuilder(Class.forName(c)).createMock();
      } catch (ClassNotFoundException e) {
        // 0.20 case
        return createMockBuilder(Counter.class).createMock();
      }
    }

    @Override
    public Counter getCounter(Enum<?> name) {
      if (!counters.containsKey(name)) {
        counters.put(name, newCounter());
      }
      return counters.get(name);
    }


    @Override
    public Counter getCounter(String group, String name) {
      if (!counterGroups.containsKey(group + name)) {
        counterGroups.put(group + name, newCounter());
      }
      return counterGroups.get(group+name);
    }

    @Override
    public void progress() {
    }

    @Override
    public void setStatus(String status) {
    }

    public float getProgress() {
      return 0;
    }

  }

}
