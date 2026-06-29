CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_active_assignment
ON vehicle_assignments(vehicle_id)
WHERE active = true;
