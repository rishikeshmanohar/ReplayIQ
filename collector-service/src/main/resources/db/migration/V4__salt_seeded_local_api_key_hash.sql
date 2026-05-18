UPDATE projects
SET api_key_hash = 'sha256:ZGVidWdmbG93LWxvY2FsLWRldi1zYWx0:R3HiANpJctglpE0ISsDGfO9OIRy7mOm4uwGm8Sv-b9A'
WHERE id = 1
  AND api_key_hash IN (
      '75666eca7e48000f9b2b1a58e6fbf6a014187f6b5bf62c6fd1dd3ef46b3b8e80',
      'local-dev-api-key-hash'
  );
