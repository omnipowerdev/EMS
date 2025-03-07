package io.openems.edge.goodwe.charger.singlestring;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class GoodWeChargerPv2Test {

	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeChargerPv2()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("ess0") //
						.setModbusId("modbus0") //
						.build());
	}
}
