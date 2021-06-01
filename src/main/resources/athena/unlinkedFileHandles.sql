WITH F AS (
	SELECT distinct id FROM ${stack}${instance}fileHandleDataRecords WHERE updatedOn < NOW() - INTERVAL '30' DAY AND isPreview = false AND status = 'AVAILABLE'
)
SELECT F.id FROM F LEFT JOIN ${stack}${instance}fileHandleAssociationsRecords A ON F.id = A.fileHandleId WHERE A.fileHandleId IS NULL