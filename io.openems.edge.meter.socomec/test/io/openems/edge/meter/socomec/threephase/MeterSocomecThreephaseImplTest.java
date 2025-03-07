package io.openems.edge.meter.socomec.threephase;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterSocomecThreephaseImplTest {

	private static MeterSocomecThreephaseImpl meter;

	@Before
	public void setup() throws Exception {
		meter = new MeterSocomecThreephaseImpl();
		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(false) //
						.build()); //
	}

	@Test
	public void testCountisE14() throws Exception {
		meter.identifiedCountisE14();
	}

	@Test
	public void testCountisE23_E24_E27_E28() throws Exception {
		meter.identifiedCountisE23_E24_E27_E28();
	}

	@Test
	public void testCountisE34_E44() throws Exception {
		meter.identifiedCountisE34_E44();
	}

	@Test
	public void testDirisA10() throws Exception {
		meter.identifiedDirisA10();
	}

	@Test
	public void testDirisA14() throws Exception {
		meter.identifiedDirisA14();
	}

	@Test
	public void testDirisB30() throws Exception {
		meter.identifiedDirisB30();
	}

}
