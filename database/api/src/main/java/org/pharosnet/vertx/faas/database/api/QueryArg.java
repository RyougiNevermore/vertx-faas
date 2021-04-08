package org.pharosnet.vertx.faas.database.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@DataObject(generateConverter = true)
public class QueryArg {

    public QueryArg() {
    }

    public QueryArg(JsonObject jsonObject) {
        QueryArgConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        QueryArgConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String query;
    private JsonArray args;
    private Boolean batch;
    private Boolean slaverMode;
    private Boolean needLastInsertedId;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public JsonArray getArgs() {
        return args;
    }

    public void setArgs(JsonArray args) {
        this.args = args;
    }

    public Boolean getBatch() {
        return batch;
    }

    public void setBatch(Boolean batch) {
        this.batch = batch;
    }

    public Boolean getSlaverMode() {
        return slaverMode;
    }

    public void setSlaverMode(Boolean slaverMode) {
        this.slaverMode = slaverMode;
    }

    public Boolean getNeedLastInsertedId() {
        return needLastInsertedId;
    }

    public void setNeedLastInsertedId(Boolean needLastInsertedId) {
        this.needLastInsertedId = needLastInsertedId;
    }

    public String toString() {
        return this.toJson().encodePrettily();
    }

    public Tuple toSQLArg() {
        return this.toSQLArg(this.args);
    }

    protected Tuple toSQLArg(JsonArray args) {
        Tuple tuple = Tuple.tuple();
        for (int i = 0; i < args.size(); i++) {
            JsonObject arg = args.getJsonObject(i);
            if (arg.getValue("value") == null) {
                tuple.addValue(null);
                continue;
            }
            String valueType = Optional.ofNullable(arg.getString("type")).orElse("").trim();
            if (valueType.equals("String") || valueType.equals("Enum") || valueType.equals("Duration")) {
                tuple.addValue(arg.getString("value"));
            } else if (valueType.equals("Boolean")) {
                tuple.addValue(arg.getBoolean("value"));
            } else if (valueType.equals("Integer")) {
                tuple.addValue(arg.getInteger("value"));
            } else if (valueType.equals("Long")) {
                tuple.addValue(arg.getLong("value"));
            } else if (valueType.equals("Float")) {
                tuple.addValue(arg.getFloat("value"));
            } else if (valueType.equals("Double")) {
                tuple.addValue(arg.getDouble("value"));
            } else if (valueType.equals("Instant")) {
                tuple.addValue(arg.getInstant("value"));
            } else if (valueType.equals("LocalDate")) {
                tuple.addLocalDate(LocalDate.parse(arg.getString("value")));
            } else if (valueType.equals("LocalTime")) {
                tuple.addLocalTime(LocalTime.parse(arg.getString("value")));
            } else if (valueType.equals("LocalDateTime")) {
                tuple.addLocalDateTime(LocalDateTime.parse(arg.getString("value")));
            } else if (valueType.equals("OffsetDateTime")) {
                tuple.addOffsetDateTime(OffsetDateTime.parse(arg.getString("value")));
            } else if (valueType.equals("OffsetTime")) {
                tuple.addOffsetTime(OffsetTime.parse(arg.getString("value")));
            } else if (valueType.equals("JsonObject")) {
                tuple.addJsonObject(arg.getJsonObject("value"));
            } else if (valueType.equals("JsonArray")) {
                tuple.addJsonArray(arg.getJsonArray("value"));
            } else {
                tuple.addValue(arg.getValue("value"));
            }
        }
        return tuple;
    }

    public List<Tuple> toSQLBatchArg() {
        List<Tuple> tuples = new ArrayList<>();
        if (this.batch) {
            for (int i = 0; i < this.args.size(); i++) {
                JsonArray batchArg = this.args.getJsonArray(i);
                tuples.add(this.toSQLArg(batchArg));
            }
        }
        return tuples;
    }

    public static JsonObject mapArg(Object value) {
        JsonObject arg = new JsonObject();
        if (value == null) {
            arg.put("type", "Null").put("value", null);
            return arg;
        }
        if (value instanceof String) {
            arg.put("type", "String").put("value", value);
        } else if (value instanceof Enum) {
            arg.put("type", "Enum").put("value", value.toString());
        } else if (value instanceof Boolean) {
            arg.put("type", "Boolean").put("value", value);
        } else if (value instanceof Integer) {
            arg.put("type", "Integer").put("value", value);
        } else if (value instanceof Long) {
            arg.put("type", "Long").put("value", value);
        } else if (value instanceof Float) {
            arg.put("type", "Float").put("value", value);
        } else if (value instanceof Double) {
            arg.put("type", "Double").put("value", value);
        } else if (value instanceof Instant) {
            arg.put("type", "Instant").put("value", value);
        } else if (value instanceof LocalDate) {
            arg.put("type", "LocalDate").put("value", value.toString());
        } else if (value instanceof LocalTime) {
            arg.put("type", "LocalTime").put("value", value.toString());
        } else if (value instanceof LocalDateTime) {
            arg.put("type", "LocalDateTime").put("value", value.toString());
        } else if (value instanceof OffsetDateTime) {
            arg.put("type", "OffsetDateTime").put("value", value.toString());
        } else if (value instanceof OffsetTime) {
            arg.put("type", "OffsetTime").put("value", value.toString());
        } else if (value instanceof Duration) {
            arg.put("type", "Duration").put("value", value.toString());
        } else if (value instanceof JsonObject) {
            arg.put("type", "JsonObject").put("value", value);
        } else if (value instanceof JsonArray) {
            arg.put("type", "JsonArray").put("value", value);
        } else {
            arg.put("type", "Unknown").put("value", value);
        }
        return arg;
    }
}
