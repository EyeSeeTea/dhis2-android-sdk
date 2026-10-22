# Creates table CategoryOptionOrganisationUnitLink
CREATE TABLE CategoryOptionOrganisationUnitLink (_id INTEGER PRIMARY KEY AUTOINCREMENT, categoryOption TEXT NOT NULL, organisationUnit TEXT NOT NULL, FOREIGN KEY (categoryOption) REFERENCES CategoryOption (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, FOREIGN KEY (organisationUnit) REFERENCES OrganisationUnit (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, UNIQUE (categoryOption, organisationUnit));

# EyeSeeTea customization - URL Data Element Field
ALTER TABLE DataElement ADD COLUMN url TEXT;
