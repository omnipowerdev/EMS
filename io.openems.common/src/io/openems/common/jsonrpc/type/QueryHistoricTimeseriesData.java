package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.IntStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserChannelAddress;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesData.Request;
import io.openems.common.jsonrpc.type.QueryHistoricTimeseriesData.Response;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public class QueryHistoricTimeseriesData implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "queryHistoricTimeseriesData";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution // null-able
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link QueryHistoricTimeseriesData}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				final var timezone = json.getObject("timezone", QueryHistoricTimeseriesData.zoneIdSerializer());
				final var resolution = json.getObject("resolution", QueryHistoricTimeseriesData.resolutionSerializer());

				return new Request(//
						json.getLocalDate("fromDate").atStartOfDay(timezone), //
						json.getLocalDate("toDate").atStartOfDay(timezone).plusDays(1), //
						json.getSet("channels", JsonElementPath::getAsChannelAddress), //
						resolution //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("fromDate", obj.fromDate().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.addProperty("toDate", obj.toDate().minusDays(1).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
						.add("channels", obj.channels().stream() //
								.map(Object::toString) //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray())) //
						.add("timezone",
								QueryHistoricTimeseriesData.zoneIdSerializer().serialize(obj.fromDate().getZone())) //
						.add("resolution",
								QueryHistoricTimeseriesData.resolutionSerializer().serialize(obj.resolution())) //
						.build();
			});
		}

	}

	public record Response(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryHistoricTimeseriesData.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<QueryHistoricTimeseriesData.Response> serializer() {
			return jsonObjectSerializer(QueryHistoricTimeseriesData.Response.class, json -> {

				final var a = json.getJsonObjectPath("data").collect(new StringParserChannelAddress(),
						toMap(Entry::getKey, t -> t.getValue().getAsJsonArrayPath().collect(toUnmodifiableList())));
				final var timestamps = json.getList("timestamps",
						t -> t.getAsStringPathZonedDateTime(DateTimeFormatter.ISO_INSTANT).get());

				final var resultMap = IntStream.range(0, timestamps.size()) //
						.mapToObj(i -> {
							final var timestamp = timestamps.get(i);
							final SortedMap<ChannelAddress, JsonElement> b = a.entrySet().stream() //
									.collect(toMap(t -> t.getKey().get(),
											t -> t.getValue().get(i).getAsJsonPrimitivePath().get(), (t, u) -> u,
											TreeMap::new));
							return Map.entry(timestamp, b);
						}).collect(toMap(t -> t.getKey(), t -> t.getValue(), (t, u) -> u, TreeMap::new));

				return new Response(resultMap);
			}, obj -> {
				final var data = new JsonObject();
				for (final var rowEntry : obj.table().entrySet()) {
					for (final var colEntry : rowEntry.getValue().entrySet()) {
						var channelAddress = colEntry.getKey().toString();
						var value = colEntry.getValue();
						var channelValuesElement = data.get(channelAddress);
						JsonArray channelValues;
						if (channelValuesElement != null) {
							channelValues = channelValuesElement.getAsJsonArray();
						} else {
							channelValues = new JsonArray();
						}
						channelValues.add(value);
						data.add(channelAddress, channelValues);
					}
				}
				return JsonUtils.buildJsonObject() //
						.add("timestamps", obj.table().keySet().stream() //
								.map(t -> new JsonPrimitive(t.format(DateTimeFormatter.ISO_INSTANT))) //
								.collect(toJsonArray()))
						.add("data", data) //
						.build();
			});
		}

	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link QueryHistoricTimeseriesEnergy}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<ZoneId> zoneIdSerializer() {
		return jsonSerializer(ZoneId.class, json -> {
			return json.multiple(List.of(//
					// For UI version before 2022.4.0
					new JsonElementPath.Case<>(JsonElementPath::isNumber, t -> {
						return ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(t.getAsInt() * -1));
					}), //
					new JsonElementPath.Case<>(t -> true, t -> {
						return TimeZone.getTimeZone(t.getAsString()).toZoneId();
					}) //
			));
		}, obj -> {
			return new JsonPrimitive(obj.toString());
		});
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link QueryHistoricTimeseriesEnergy}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Resolution> resolutionSerializer() {
		return jsonSerializer(Resolution.class, json -> {
			return json.multiple(List.of(//
					// For UI version before 2022.4.0
					new JsonElementPath.Case<>(t -> /* t.isPresent() && */t.isJsonPrimitive(), t -> {
						return new Resolution(t.getAsInt(), ChronoUnit.SECONDS);
					}), //
					new JsonElementPath.Case<>(t -> true, t -> {
						final var obj = t.getAsJsonObjectPath();
						return new Resolution(obj.getInt("value"), obj.getEnum("unit", ChronoUnit.class));
					}) //
			));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("value", obj.getValue()) //
					.addProperty("unit", obj.getUnit()) //
					.build();
		});
	}

}
