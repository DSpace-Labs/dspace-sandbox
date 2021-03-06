--
-- database_schema_14-15.sql
--
-- Version: $$
--
-- Date:    $Date:$
--
-- Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
-- Institute of Technology.  All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
-- 
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
-- 
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
-- 
-- - Neither the name of the Hewlett-Packard Company nor the name of the
-- Massachusetts Institute of Technology nor the names of their
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.

--
-- SQL commands to upgrade the database schema of a live DSpace 1.4.2
-- to the DSpace 1.5 database schema
-- 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. 

-- Remove NOT NULL restrictions from the checksum columns of most_recent_checksum
ALTER TABLE most_recent_checksum MODIFY expected_checksum null;
ALTER TABLE most_recent_checksum MODIFY current_checksum null;

------------------------------------------------------
-- New Column language language in EPerson
------------------------------------------------------

alter table eperson add column language VARCHAR2(64);
update eperson set language = 'en';

alter table bundle drop column mets_bitstream_id; -- totally unused column

-------------------------------------------------------------------------------
-- Necessary for Configurable Submission functionality:
-- Modification to workspaceitem table to support keeping track
-- of the last page reached within a step in the Configurable Submission Process
-------------------------------------------------------------------------------
ALTER TABLE workspaceitem ADD page_reached INTEGER;

-------------------------------------------------------------------------
-- Tables to manage cache of item counts for communities and collections
-------------------------------------------------------------------------

CREATE TABLE collection_item_count (
	collection_id INTEGER PRIMARY KEY REFERENCES collection(collection_id),
	count INTEGER
);

CREATE TABLE community_item_count (
	community_id INTEGER PRIMARY KEY REFERENCES community(community_id),
	count INTEGER
);

------------------------------------------------------------------
-- Remove sequences and tables of the old browse system
------------------------------------------------------------------

DROP SEQUENCE itemsbyauthor_seq;
DROP SEQUENCE itemsbytitle_seq;
DROP SEQUENCE itemsbydate_seq;
DROP SEQUENCE itemsbydateaccessioned_seq;
DROP SEQUENCE itemsbysubject_seq;

DROP TABLE ItemsByAuthor CASCADE;
DROP TABLE ItemsByTitle CASCADE;
DROP TABLE ItemsByDate CASCADE;
DROP TABLE ItemsByDateAccessioned CASCADE;
DROP TABLE ItemsBySubject CASCADE;
