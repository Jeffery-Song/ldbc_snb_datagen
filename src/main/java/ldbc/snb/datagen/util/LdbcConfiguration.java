package ldbc.snb.datagen.util;

import ldbc.snb.datagen.hadoop.writer.HdfsCsvWriter;
import ldbc.snb.datagen.serializer.DynamicActivitySerializer;
import ldbc.snb.datagen.serializer.DynamicPersonSerializer;
import ldbc.snb.datagen.serializer.StaticSerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.activity.CsvBasicDynamicActivitySerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.activity.CsvCompositeDynamicActivitySerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.activity.CsvCompositeMergeForeignDynamicActivitySerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.activity.CsvMergeForeignDynamicActivitySerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.person.CsvBasicDynamicPersonSerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.person.CsvCompositeDynamicPersonSerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.person.CsvCompositeMergeForeignDynamicPersonSerializer;
import ldbc.snb.datagen.serializer.snb.csv.dynamicserializer.person.CsvMergeForeignDynamicPersonSerializer;
import ldbc.snb.datagen.serializer.snb.csv.staticserializer.CsvBasicStaticSerializer;
import ldbc.snb.datagen.serializer.snb.csv.staticserializer.CsvCompositeMergeForeignStaticSerializer;
import ldbc.snb.datagen.serializer.snb.csv.staticserializer.CsvCompositeStaticSerializer;
import ldbc.snb.datagen.serializer.snb.csv.staticserializer.CsvMergeForeignStaticSerializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.StringUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

public class LdbcConfiguration implements Iterable<Map.Entry<String, String>>, Serializable {
    public final Map<String, String> map;

    public LdbcConfiguration(Map<String, String> map) {
        this.map = map;
    }

    public String get(String key) {
        return map.get(key);
    }

    public String get(String key, String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String valueString = this.getTrimmed(key);
        if (null != valueString && !valueString.isEmpty()) {
            if (org.apache.hadoop.util.StringUtils.equalsIgnoreCase("true", valueString)) {
                return true;
            } else {
                return StringUtils.equalsIgnoreCase("false", valueString) ? false : defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public int getInt(String name, int defaultValue) {
        String valueString = this.getTrimmed(name);
        if (valueString == null) {
            return defaultValue;
        } else {
            String hexString = this.getHexDigits(valueString);
            return hexString != null ? Integer.parseInt(hexString, 16) : Integer.parseInt(valueString);
        }
    }

    public double getDouble(String name, double defaultValue) {
        String valueString = this.getTrimmed(name);
        return valueString == null ? defaultValue : Double.parseDouble(valueString);
    }

    public float getFloat(String name, float defaultValue) {
        String valueString = this.getTrimmed(name);
        return valueString == null ? defaultValue : Float.parseFloat(valueString);
    }

    private String getHexDigits(String value) {
        boolean negative = false;
        String str = value;
        String hexString;
        if (value.startsWith("-")) {
            negative = true;
            str = value.substring(1);
        }

        if (!str.startsWith("0x") && !str.startsWith("0X")) {
            return null;
        } else {
            hexString = str.substring(2);
            if (negative) {
                hexString = "-" + hexString;
            }

            return hexString;
        }
    }

    public String getTrimmed(String name) {
        String value = this.get(name);
        return null == value ? null : value.trim();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.map.entrySet().iterator();
    }

    public DynamicPersonSerializer<HdfsCsvWriter> getDynamicPersonSerializer() {

        String serializerFormat = map.get("serializer.format");

        DynamicPersonSerializer<HdfsCsvWriter> output;
        switch (serializerFormat) {
            case "CsvBasic":
                output = new CsvBasicDynamicPersonSerializer();
                break;
            case "CsvMergeForeign":
                output = new CsvMergeForeignDynamicPersonSerializer();
                break;
            case "CsvComposite":
                output = new CsvCompositeDynamicPersonSerializer();
                break;
            case "CsvCompositeMergeForeign":
                output = new CsvCompositeMergeForeignDynamicPersonSerializer();
                break;
            default:
                throw new IllegalStateException("Unexpected person serializer: " + serializerFormat);
        }

        return output;
    }

    public DynamicActivitySerializer<HdfsCsvWriter> getDynamicActivitySerializer() {

        String serializerFormat = map.get("serializer.format");

        DynamicActivitySerializer<HdfsCsvWriter> output;
        switch (serializerFormat) {
            case "CsvBasic":
                output = new CsvBasicDynamicActivitySerializer();
                break;
            case "CsvMergeForeign":
                output = new CsvMergeForeignDynamicActivitySerializer();
                break;
            case "CsvComposite":
                output = new CsvCompositeDynamicActivitySerializer();
                break;
            case "CsvCompositeMergeForeign":
                output = new CsvCompositeMergeForeignDynamicActivitySerializer();
                break;
            default:
                throw new IllegalStateException("Unexpected activity serializer: " + serializerFormat);
        }

        return output;
    }

    public StaticSerializer<HdfsCsvWriter> getStaticSerializer() {

        String serializerFormat = map.get("serializer.format");

        StaticSerializer<HdfsCsvWriter> output;
        switch (serializerFormat) {
            case "CsvBasic":
                output = new CsvBasicStaticSerializer();
                break;
            case "CsvComposite":
                output = new CsvCompositeStaticSerializer();
                break;
            case "CsvCompositeMergeForeign":
                output = new CsvCompositeMergeForeignStaticSerializer();
                break;
            case "CsvMergeForeign":
                output = new CsvMergeForeignStaticSerializer();
                break;
            default:
                throw new IllegalStateException("Unexpected static serializer: " + serializerFormat);
        }

        return output;
    }

    public boolean isCompressed() {

        return Boolean.parseBoolean(map.get("serializer.compressed"));

    }

    public boolean insertTrailingSeparator() {
        return Boolean.parseBoolean(map.get("serializer.insertTrailingSeparator"));

    }

    public String getOutputDir(){
        return map.get("serializer.outputDir");
    }

    public String getBuildDir() {
        return map.get("serializer.buildDir");
    }

    public String getSocialNetworkDir() {
        return map.get("serializer.socialNetworkDir");
    }

    public void printConfig() {
        System.out.println("********* Configuration *********");
        map.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("*********************************");
    }
}
