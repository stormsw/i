package org.igov.util.JSON;

/**
 * User: goodg_000
 * Date: 21.06.2015
 * Time: 14:24
 */
public class JsonDateDeserializer extends AbstractJsonDateTimeDeserializer {

    public JsonDateDeserializer() {
        super(JsonDateSerializer.DATE_FORMATTER);
    }
}
