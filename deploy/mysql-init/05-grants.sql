-- schema.sql recrea la base u_medical; esto garantiza que el usuario de la aplicacion siga teniendo permisos.
GRANT ALL PRIVILEGES ON u_medical.* TO 'umedical'@'%';
FLUSH PRIVILEGES;
