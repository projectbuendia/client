package org.msf.records.location;

import android.test.AndroidTestCase;

import org.msf.records.model.Zone;
import org.msf.records.net.model.Location;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class LocationTreeTest extends AndroidTestCase {

	public void testConstruction_ShouldSucceed() {
		// WHEN we construct a location tree from some dummy data
		ImmutableList<Location> locations = ImmutableList.of(
				newLocation("root", "", null),
				newLocation("Probable Zone", Zone.PROBABLE_ZONE_UUID, ""));
		ImmutableMap<String, Integer> patientCountByUuid = ImmutableMap.of(
				Zone.PROBABLE_ZONE_UUID, 3);
		// THEN it should succeed
		new LocationTree(
				getContext().getResources(),
				locations,
				patientCountByUuid);
	}

	private Location newLocation(String name, String uuid, String parentUuid) {
		Location location = new Location();
		location.names = ImmutableMap.of("en", name);
		location.uuid = uuid;
		location.parent_uuid = parentUuid;
		return location;
	}
}
