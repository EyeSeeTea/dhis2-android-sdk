# Creates table CategoryOptionOrganisationUnitLink - migration 85 in the future
CREATE TABLE CategoryOptionOrganisationUnitLink (_id INTEGER PRIMARY KEY AUTOINCREMENT, categoryOption TEXT NOT NULL, organisationUnit TEXT NOT NULL, FOREIGN KEY (categoryOption) REFERENCES CategoryOption (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, FOREIGN KEY (organisationUnit) REFERENCES OrganisationUnit (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, UNIQUE (categoryOption, organisationUnit));

# Creates tables TrackedEntityInstanceFilter and TrackedEntityInstanceEventFilter
CREATE TABLE TrackedEntityInstanceFilter (_id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT NOT NULL UNIQUE, code TEXT, name TEXT, displayName TEXT, created TEXT, lastUpdated TEXT, color TEXT, icon TEXT, program TEXT NOT NULL, description TEXT, sortOrder INTEGER, enrollmentStatus TEXT, followUp INTEGER, periodFrom INTEGER, periodTo INTEGER, FOREIGN KEY (program) REFERENCES Program (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED);
CREATE TABLE TrackedEntityInstanceEventFilter (_id INTEGER PRIMARY KEY AUTOINCREMENT, trackedEntityInstanceFilter TEXT NOT NULL, programStage TEXT, eventStatus TEXT, periodFrom INTEGER, periodTo INTEGER, assignedUserMode TEXT, FOREIGN KEY (trackedEntityInstanceFilter) REFERENCES TrackedEntityInstanceFilter (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED);


