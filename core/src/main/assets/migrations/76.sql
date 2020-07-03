CREATE TABLE DataElementLegendSetLink (_id INTEGER PRIMARY KEY AUTOINCREMENT, dataElement TEXT NOT NULL, legendSet TEXT NOT NULL, FOREIGN KEY (dataElement) REFERENCES DataElement (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, FOREIGN KEY (legendSet) REFERENCES LegendSet (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, UNIQUE (dataElement, legendSet));
