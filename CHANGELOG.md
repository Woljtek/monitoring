# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Content of release :
> - **Added** for new features.
> - **Changed** for changes in existing functionality.
> - **Deprecated** for soon-to-be removed features.
> - **Removed** for now removed features.
> - **Fixed** for any bug fixes.
> - **Security** in case of vulnerabilities.

## [1.5.0-rc1] - 2023-01-04
### Added
- [#735 - [MONITORING] Add information "late" and "duplicate" for each product.](https://github.com/COPRS/rs-issues/issues/735)
- Set javadoc and comment
- Store in cache processor description for the same configuration to avoid doing same job repeatedly
- Added json entry in application logs, describing the behaviour of the application : entities stored in database, update, unchanged as well as processing and database storage times 
- Update configuration file of trace-filter and trace-ingestor to handle s2-l1 processing (#492)
### Deprecated
- Remove old behavior on trace parser
### Fix
- [#749 - [BUG] [RS core Monitoring] does not ingest most of the processing.](https://github.com/COPRS/rs-issues/issues/749)
- [#755 - [BUG] [TRACE] [RS core Monitoring] Field "duplicate" not set to "true" when several processing are linked to same chunk or DSIB](https://github.com/COPRS/rs-issues/issues/755)
- Use OneToOne relation instead of ManyToOne between Product and InputListInternal/OutputList
- Extract leaf for each level of the tree and not only the first one
- Use alternate check to avoid DoS with regex backtracking
- Minor fixes
- Remove archive extension on aux data ingestion trigger/worker and prip worker product configuration
- Use DefaultListableBeanFactory to register new bean configuration instance
- Remove "late" column for product (feature #735)

## [1.4.0-rc1] - 2022-11-23
### Added
- [#484 - [RS core Monitoring] Handle duplicate production RULE n°3 (PRODUCT contamination)](https://github.com/COPRS/rs-issues/issues/484)
- [#485 - [RS core Monitoring] Handle duplicate production RULE n°4 (PROCESSING contamination)](https://github.com/COPRS/rs-issues/issues/485)
### Fixed
- Fix build of FINOPS services

## [1.3.0-rc1] - 2022-10-26
### Added
- [#483 - [RS core Monitoring] Handle duplicate production RULE n°2 (same PRODUCT input)](https://github.com/COPRS/rs-issues/issues/483)
- [#482 - [RS core Monitoring] Handle duplicate production RULE n°1 (CHUNK & DSIB)](https://github.com/COPRS/rs-issues/issues/482)
- [#564 - [MONITORING] Set date fields in the database](https://github.com/COPRS/rs-issues/issues/564)

## [1.2.0-rc1] - 2022-09-28
### Added
- Documentation and Notice.md
### Changed
- Code Quality improvements

## [1.1.0-rc1] - 2022-08-31
### Added
- [#412 - [RS core Monitoring] Compute trace from RS add-on / ExecutionWorker](https://github.com/COPRS/rs-issues/issues/412)

## [0.10.0-rc1] - 2022-08-03
### Added
- [#355 - [RS core Monitoring] Feed SQL data with information from ingestion service traces](https://github.com/COPRS/rs-issues/issues/355)
- [#411 - [RS core Monitoring] Compute product information from RS core Metadata Extraction trace (catalog data)](https://github.com/COPRS/rs-issues/issues/411)

## [0.9.0-rc1] - 2022-07-06
### Added
- [#358 - [MONITORING] Create a RS core TRACE skeleton](https://github.com/COPRS/rs-issues/issues/358)

## [0.3.0-rc1] - 2022-01-19
### Added
- [#175 - FinOPS: monitor & control system costs - RESOURCES](https://github.com/COPRS/rs-issues/issues/175)
- [#187 - FinOPS: monitor & control system costs - STORAGE](https://github.com/COPRS/rs-issues/issues/187)
