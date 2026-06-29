UPDATE projects
SET api_key_hash = 'sha256:ZmFpbGZyYW1lLWxvY2FsLWRldi1zYWx0:uNVeaLs765-HWmCup9ui_femCqLD2ccW9_wZOGbkpPE'
WHERE id = 1
  AND api_key_hash IN (
      'e07bc6524d40a0c7ca8789206007d05ef7f8195850b5f4389b93c5d27e571033',
      'local-dev-api-key-hash'
  );
