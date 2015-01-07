-- A raw query that fetches localized location information for a given locale.
--
-- Parameters:
--   string, the locale in which the location information should be returned
--
-- Result Columns:
--   string location_uuid, the UUID of a location
--   string parent_uuid, the UUID of the location's parent
--   string name, the localized name of the location

SELECT
  locations.location_uuid as location_uuid,
  locations.parent_uuid as parent_uuid,
  location_names.name as name
FROM locations
  INNER JOIN location_names
    ON locations.location_uuid = location_names.location_uuid
WHERE
  location_names.locale = ?
