package org.zstack.utils.form;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.TypeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Form<T> {
    private final Class<T> clz;
    private String base64Content;

    private Map<String, Extender> columnExtender = new HashMap<>();
    private Map<String, Consumer<T>> columnConsumer = new HashMap<>();
    private Map<Field, Param> fieldParam = new HashMap<>();
    private String[] columns;

    public interface Consumer<T> {
        void accept(T item, String value) throws Exception;
    }

    public interface Extender {
        Collection<String> accept(String value);
    }

    private Form(Class<T> clz, String base64Content) {
        this.clz = clz;
        this.base64Content = base64Content;
    }

    public static <T> Form<T> New(Class<T> clazz, String base64Content) {
        return new Form<>(clazz, base64Content);
    }

    public Form<T> addConverter(String column, Extender extender, Consumer<T> consumer) {
        columnExtender.put(column, extender);
        columnConsumer.put(column, consumer);
        return this;
    }

    public Form<T> addConverter(String column, Consumer<T> consumer) {
        columnConsumer.put(column, consumer);
        return this;
    }

    public List<T> load() throws IOException {
        List<T> results = new ArrayList<>();

        FormReader reader = getFormReader(getReaderType());
        columns = reader.getHeader();
        if (columns == null || columns.length == 0) {
            return null;
        }
        produceDefaultColumnConsumer();
        produceParamCheck();

        String[] record;
        int line = 0;
        Map<Integer, String> lineErrorInfo = new TreeMap<>();
        while ((record = reader.nextRecord()) != null) {
            line++;
            if (record.length == 0) {
                continue;
            }

            try {
                if (columnExtender.isEmpty()) {
                    results.add(loadObject(record));
                } else {
                    results.addAll(new ExtendRecordLoader(record).loadExtendRecord());
                }
            } catch (Exception e) {
                lineErrorInfo.put(line, e.getMessage());
            }
        }

        if (!lineErrorInfo.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", lineErrorInfo.entrySet().stream()
                    .map(e -> String.format("line %d: %s", e.getKey(), e.getValue()))
                    .collect(Collectors.toList())));
        }

        return results;
    }

    private FormType getReaderType() throws IOException {
        return ExcelReader.checkType(base64Content) ? FormType.Excel : FormType.CSV;
    }

    private FormReader getFormReader(FormType type){
        if (type == FormType.CSV) {
            return new CsvReader(base64Content);
        } else if (type == FormType.Excel) {
            return new ExcelReader(base64Content);
        }

        throw new RuntimeException(String.format("not support form type: %s", type));
    }

    private class ExtendRecordLoader {
        List<T> objects = new ArrayList<>();
        String[][] extendRecord;

        ExtendRecordLoader(String[] originRecord) {
            this.extendRecord = extendRecord(originRecord);
        }

        private List<T> loadExtendRecord() throws Exception {
            stepLoadExtendRecord(new String[extendRecord.length], 0);
            return objects;
        }

        private void stepLoadExtendRecord(String[] finalRecord, int stepIndex) throws Exception {
            for (String value : extendRecord[stepIndex]) {
                finalRecord[stepIndex] = value;
                if (stepIndex == finalRecord.length - 1) {
                    objects.add(loadObject(finalRecord));
                } else {
                    stepLoadExtendRecord(finalRecord, stepIndex + 1);
                }
            }
        }

        private String[][] extendRecord(String[] originRecord){
            String[][] result = new String[originRecord.length][];
            for (int i = 0; i < originRecord.length; i++) {
                Extender extender = columnExtender.get(columns[i]);
                if (extender != null && StringUtils.isNotEmpty(originRecord[i])) {
                    Collection<String> values = extender.accept(originRecord[i]);
                    result[i] = values.toArray(new String[values.size()]);
                } else {
                    result[i] = new String[] {originRecord[i]};
                }
            }
            return result;
        }
    }

    private T loadObject(String[] record) throws Exception {
        T object = clz.newInstance();
        for (int i = 0; i < record.length; i++) {
            String key = columns[i];
            String value = record[i];
            Consumer<T> consumer = columnConsumer.get(key);
            if (consumer != null && StringUtils.isNotEmpty(value)) {
                consumer.accept(object, value);
            }
        }

        return checkParam(object);
    }

    private T checkParam(T object) throws IllegalAccessException {
        for (Map.Entry<Field, Param> entry : fieldParam.entrySet()) {
            Field f = entry.getKey();
            Param param = entry.getValue();

            f.setAccessible(true);
            Object value = f.get(object);
            if (param.required() && value == null) {
                throw new IllegalArgumentException(String.format("field[%s] cannot be null", f.getName()));
            }

            if (value != null && value instanceof String && !param.noTrim()) {
                f.set(object, ((String) value).trim());
            }

            if (value != null && param.numberRange().length > 0 && TypeUtils.isTypeOf(value, Integer.TYPE, Integer.class, Long.TYPE, Long.class)) {
                DebugUtils.Assert(param.numberRange().length == 2, String.format("invalid field[%s], Param.numberRange must have and only have 2 items", f.getName()));
                long low = param.numberRange()[0];
                long high = param.numberRange()[1];
                long val = ((Number) value).longValue();
                if (val < low || val > high) {
                    throw new IllegalArgumentException(String.format("field[%s] must be in range of [%s, %s]", f.getName(), low, high));
                }
            }
        }

        return object;
    }

    private void produceParamCheck() {
         List<Field> fields = FieldUtils.getAnnotatedFields(Param.class, clz);
         fields.forEach(it -> fieldParam.put(it, it.getAnnotation(Param.class)));
    }

    private void produceDefaultColumnConsumer(){
        List<String> cols = Arrays.asList(columns);
        for (Field field : FieldUtils.getAllFields(clz)) {
            if (columnConsumer.keySet().contains(field.getName()) || !cols.contains(field.getName())) {
                continue;
            }
            Consumer<T> setter = getDefaultSetter(field);
            Optional.ofNullable(setter).ifPresent(it -> columnConsumer.put(field.getName(), it));
        }
    }

    private Consumer<T> getDefaultSetter(Field f) {
        f.setAccessible(true);
        if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).intValue());
        } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).longValue());
        } else if (Float.class.isAssignableFrom(f.getType()) || Float.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value).floatValue());
        } else if (Double.class.isAssignableFrom(f.getType()) || Double.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Double.valueOf(value));
        } else if (String.class.isAssignableFrom(f.getType())) {
            return f::set;
        } else if (Boolean.class.isAssignableFrom(f.getType()) || Boolean.TYPE.isAssignableFrom(f.getType())) {
            return (it, value) -> f.set(it, Boolean.valueOf(value.toLowerCase()));
        } else {
            return null;
        }
    }
}
