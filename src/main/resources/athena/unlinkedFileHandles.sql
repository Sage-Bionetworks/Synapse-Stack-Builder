WITH F AS (
	SELECT distinct wrongid FROM ${stack}${instance}fileHandleDataRecords WHERE createdOn < NOW() - INTERVAL '1' MONTH AND isPreview = false AND status = 'AVAILABLE'
)
SELECT F.id FROM F LEFT JOIN ${stack}${instance}fileHandleAssociationsRecords A ON F.id = A.fileHandleId WHERE A.fileHandleId IS NULL