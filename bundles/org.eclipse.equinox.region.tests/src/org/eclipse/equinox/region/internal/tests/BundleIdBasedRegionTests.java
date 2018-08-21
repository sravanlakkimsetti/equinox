/*******************************************************************************
 * Copyright (c) 2011, 2013 VMware Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.region.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import org.easymock.EasyMock;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.junit.*;
import org.osgi.framework.*;

public class BundleIdBasedRegionTests {

	private static final String OTHER_REGION_NAME = "other";

	private static final String BUNDLE_SYMBOLIC_NAME = "b";

	private static final String BUNDLE_SYMBOLIC_NAME_2 = "c";

	private static final Version BUNDLE_VERSION = new Version("1");

	private static final long BUNDLE_ID = 1L;

	private static final long BUNDLE_ID_2 = 2L;

	private static final String REGION_NAME = "reg";

	private static final long TEST_BUNDLE_ID = 99L;

	private Bundle mockBundle;

	private RegionDigraph mockGraph;

	private Iterator<Region> regionIterator;

	private BundleContext mockBundleContext;

	Region mockRegion;

	Region mockRegion2;

	RegionFilter mockRegionFilter;

	private ThreadLocal<Region> threadLocal;

	private Object bundleIdToRegionMapping;

	@Before
	public void setUp() throws Exception {
		this.threadLocal = new ThreadLocal<Region>();
		this.mockBundle = EasyMock.createMock(Bundle.class);
		EasyMock.expect(this.mockBundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
		EasyMock.expect(this.mockBundle.getVersion()).andReturn(BUNDLE_VERSION).anyTimes();
		EasyMock.expect(this.mockBundle.getBundleId()).andReturn(BUNDLE_ID).anyTimes();

		this.mockBundleContext = EasyMock.createMock(BundleContext.class);
		EasyMock.expect(this.mockBundleContext.getBundle(BUNDLE_ID)).andReturn(this.mockBundle).anyTimes();

		this.mockRegion = EasyMock.createMock(Region.class);
		this.mockRegion2 = EasyMock.createMock(Region.class);

		this.mockRegionFilter = EasyMock.createMock(RegionFilter.class);

		this.regionIterator = new Iterator<Region>() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Region next() {
				return null;
			}

			@Override
			public void remove() {
				// nothing
			}
		};
		this.mockGraph = EasyMock.createMock(RegionDigraph.class);
		this.mockGraph.connect(EasyMock.isA(Region.class), EasyMock.eq(this.mockRegionFilter), EasyMock.eq(this.mockRegion));
		EasyMock.expectLastCall().anyTimes();
		this.bundleIdToRegionMapping = RegionReflectionUtils.newStandardBundleIdToRegionMapping();
	}

	private void replayMocks() {
		EasyMock.replay(this.mockBundleContext, this.mockBundle, this.mockRegion, this.mockRegion2, this.mockRegionFilter, this.mockGraph);
	}

	@After
	public void tearDown() throws Exception {
		EasyMock.verify(this.mockBundleContext, this.mockBundle, this.mockRegion, this.mockRegion2, this.mockRegionFilter, this.mockGraph);
	}

	@Test
	public void testGetName() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		assertEquals(REGION_NAME, r.getName());
	}

	private Region createDefaultBundleIdBasedRegion() {
		return createBundleIdBasedRegion(REGION_NAME);
	}

	private Region createBundleIdBasedRegion(String regionName) {
		return RegionReflectionUtils.newBundleIdBasedRegion(regionName, this.mockGraph, this.bundleIdToRegionMapping, this.mockBundleContext, this.threadLocal);
	}

	private void defaultSetUp() {
		EasyMock.expect(this.mockGraph.iterator()).andReturn(this.regionIterator).anyTimes();
		EasyMock.expect(this.mockGraph.getEdges(EasyMock.isA(Region.class))).andReturn(new HashSet<FilteredRegion>()).anyTimes();
		replayMocks();
	}

	@Test
	public void testAddBundle() throws BundleException {
		EasyMock.expect(this.mockGraph.iterator()).andReturn(this.regionIterator).anyTimes();

		HashSet<FilteredRegion> edges = new HashSet<FilteredRegion>();
		edges.add(new FilteredRegion() {

			@Override
			public Region getRegion() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public RegionFilter getFilter() {
				return mockRegionFilter;
			}
		});
		EasyMock.expect(this.mockGraph.getEdges(EasyMock.isA(Region.class))).andReturn(edges).anyTimes();
		replayMocks();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
	}

	@Test
	public void testAddExistingBundle() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		r.addBundle(this.mockBundle);
	}

	// This restriction was removed, so no exception should be thrown.
	public void testAddConflictingBundle() throws BundleException {
		defaultSetUp();

		Bundle mockBundle2 = EasyMock.createMock(Bundle.class);
		EasyMock.expect(mockBundle2.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
		EasyMock.expect(mockBundle2.getVersion()).andReturn(BUNDLE_VERSION).anyTimes();
		EasyMock.expect(mockBundle2.getBundleId()).andReturn(BUNDLE_ID_2).anyTimes();
		EasyMock.replay(mockBundle2);

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		r.addBundle(mockBundle2);
	}

	@Test(expected = BundleException.class)
	public void testAddBundlePresentInAnotherRegion1() throws BundleException {
		Region r = regionForBundlePersentInAnotherRegionTest();
		r.addBundle(this.mockBundle);
	}

	@Test(expected = BundleException.class)
	public void testAddBundlePresentInAnotherRegion2() throws BundleException {
		Region r = regionForBundlePersentInAnotherRegionTest();
		r.addBundle(this.mockBundle.getBundleId());
	}

	private Region regionForBundlePersentInAnotherRegionTest() throws BundleException {
		this.regionIterator = new Iterator<Region>() {

			private int next = 2;

			@Override
			public boolean hasNext() {
				return this.next > 0;
			}

			@Override
			public Region next() {
				switch (next--) {
					case 2 :
						return mockRegion;
					default :
						return mockRegion2;
				}
			}

			@Override
			public void remove() {
				// nothing
			}
		};
		EasyMock.expect(this.mockGraph.iterator()).andReturn(this.regionIterator).anyTimes();
		EasyMock.expect(this.mockGraph.getEdges(EasyMock.isA(Region.class))).andReturn(new HashSet<FilteredRegion>()).anyTimes();
		EasyMock.expect(this.mockRegion.contains(EasyMock.eq(BUNDLE_ID))).andReturn(true).anyTimes();
		EasyMock.expect(this.mockRegion2.contains(EasyMock.eq(BUNDLE_ID))).andReturn(false).anyTimes();
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, BUNDLE_ID, mockRegion);

		replayMocks();

		Region r = createDefaultBundleIdBasedRegion();
		return r;
	}

	@Test
	public void testInstallBundleStringInputStream() {
		defaultSetUp();

		// TODO
	}

	@Test
	public void testInstallBundleString() {
		defaultSetUp();

		// TODO
	}

	@Test
	public void testContains() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertTrue(r.contains(this.mockBundle));
	}

	@Test
	public void testDoesNotContain() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		assertFalse(r.contains(this.mockBundle));
	}

	@Test
	public void testGetBundle() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertEquals(this.mockBundle, r.getBundle(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION));
	}

	@Test
	public void testGetBundleNotFound() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertNull(r.getBundle(BUNDLE_SYMBOLIC_NAME_2, BUNDLE_VERSION));
	}

	@Test
	public void testConnectRegion() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.connectRegion(this.mockRegion, this.mockRegionFilter);
	}

	@Test
	public void testEquals() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		Region s = createDefaultBundleIdBasedRegion();
		assertEquals(r, r);
		assertEquals(r, s);
		assertEquals(r.hashCode(), s.hashCode());
	}

	@Test
	public void testNotEqual() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		Region s = createBundleIdBasedRegion(OTHER_REGION_NAME);
		assertFalse(r.equals(s));
		assertFalse(r.equals(null));
	}

	@Test
	public void testAddRemoveBundleId() throws BundleException {
		defaultSetUp();
		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(TEST_BUNDLE_ID);
		assertTrue(r.contains(TEST_BUNDLE_ID));
		r.removeBundle(TEST_BUNDLE_ID);
		assertFalse(r.contains(TEST_BUNDLE_ID));

	}

}
