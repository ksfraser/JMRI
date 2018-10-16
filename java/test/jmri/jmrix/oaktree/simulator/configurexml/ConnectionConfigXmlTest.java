package jmri.jmrix.oaktree.simulator.configurexml;

import jmri.util.JUnitUtil;
import org.junit.*;
import jmri.jmrix.oaktree.simulator.ConnectionConfig;

/**
 * Tests for the ConnectionConfigXml class
 *
 * @author Paul Bender  Copyright (C) 2016
 */
public class ConnectionConfigXmlTest extends jmri.jmrix.configurexml.AbstractSerialConnectionConfigXmlTestBase {

    // The minimal setup for log4J
    @Before
    public void setUp() {
        JUnitUtil.setUp();
        xmlAdapter = new ConnectionConfigXml();
        cc = new ConnectionConfig();
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
        xmlAdapter = null;
        cc = null;
    }
}

