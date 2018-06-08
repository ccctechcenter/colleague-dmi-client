# Release Notes #

__1.0.9__

* Fixed a bug that prevented BatchKeys from removing records that were not found from result set

__1.0.8__

* Socket factory bug fixes. Trace logging added to socket factory.

__1.0.7__

* Support for alternate CDD view name for queries where the physical file name and CDD entries are not the same.
  For example, CAST.MIS.PARMS which is a view of the physical file ST.PARMS.

__1.0.6__

* Fix for data reads from application based files (ie ST.VALCODES)
* New methods in DmiDataService for reading valcodes and ELF translation tables

__1.0.5__

* Fix for blank lines at end of DMI transaction on certain error conditions

__1.0.4__

* Trace logging improvements when exceptions occur processing a DMI response

__1.0.3__

* Implemented CddUtils.convertFromValue(), a utility to convert from Java types to Colleague types

__1.0.2__

* Initial release