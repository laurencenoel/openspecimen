
angular.module('os.administrative.container.util', ['os.common.box'])
  .factory('ContainerUtil', function($translate, BoxLayoutUtil, NumberConverterUtil, SpecimenUtil, Util) {

    function createSpmnPos(container, label, x, y, oldOccupant) {
      return {
        occuypingEntity: 'specimen', 
        occupyingEntityName: label,
        posOne: NumberConverterUtil.fromNumber(container.columnLabelingScheme, x),
        posTwo: NumberConverterUtil.fromNumber(container.rowLabelingScheme, y),
        posOneOrdinal: x,
        posTwoOrdinal: y,
        oldOccupant: oldOccupant
      };
    }

    function getOccupantDisplayName(container, occupant) {
      if (occupant.occuypingEntity == 'specimen') {
        if (container.cellDisplayProp == 'SPECIMEN_PPID') {
          return occupant.occupantProps.ppid;
        } else if (container.cellDisplayProp == 'SPECIMEN_BARCODE' && !!occupant.occupantProps.barcode) {
          return occupant.occupantProps.barcode;
        }
      }

      return occupant.occupyingEntityName;
    }

    function getOpts(container, allowClicks, showAddMarker, useBarcode) {
      return {
        box: {
          instance             : container,
          row                  : function(occupant) { return occupant.posTwoOrdinal; },
          column               : function(occupant) { return occupant.posOneOrdinal; },
          numberOfRows         : function() { return container.noOfRows; },
          numberOfColumns      : function() { return container.noOfColumns; },
          positionLabelingMode : function() { return container.positionLabelingMode; },
          rowLabelingScheme    : function() { return container.rowLabelingScheme; },
          columnLabelingScheme : function() { return container.columnLabelingScheme; },
          occupantClick        : function() { /* dummy method to make box allow cell clicks */ }
        },
        toggleCellSelect: function() { },
        allowEmptyCellSelect: true,
        occupants: [],
        occupantName: function(occupant) {
          if (!!useBarcode && occupant.occuypingEntity == 'specimen') {
            return occupant.occupantProps.barcode || '';
          }

          return occupant.occupyingEntityName;
        },
        occupantDisplayHtml: function(occupant) {
          var displayName = undefined;
          var cssClass = '';
          if (occupant.occuypingEntity == 'specimen' && !!occupant.occupantProps) {
            displayName = getOccupantDisplayName(container, occupant);

            if (occupant.occupantProps.reserved) {
              cssClass = 'slot-reserved';
            }
          } else if (!!occupant.occupyingEntityName) {
            displayName = occupant.occupyingEntityName;
          } else if (occupant.blocked) {
            displayName = $translate.instant('container.cell_blocked');
            cssClass = 'slot-blocked';
          }

          return angular.element('<span class="slot-desc"/>')
            .addClass(cssClass).attr('title', displayName)
            .append(displayName);
        },
        allowClicks: allowClicks,
        isVacatable: function(occupant) {
          return occupant.occuypingEntity == 'specimen';
        },
        createCell: function(label, x, y, existing) {
          return createSpmnPos(container, label, x, y, existing);
        },
        onAddEvent: showAddMarker ? function() {} : undefined
      };
    }

    function getSpecimens(labels, filterOpts) {
      return SpecimenUtil.getSpecimens(labels, filterOpts).then(
        function(specimens) {
          if (!specimens) {
            return specimens;
          }

          return confirmTransferAction(!labels, specimens);
        }
      );
    }

    function confirmTransferAction(useBarcode, specimens) {
      var storedSpmns = specimens
        .filter(function(spmn) { return spmn.storageLocation && spmn.storageLocation.id > 0; })
        .map(function(spmn) { return useBarcode && spmn.barcode || spmn.label; });
      if (storedSpmns.length == 0) {
        return specimens;
      }

      return Util.showConfirm({
        title: 'container.transfer_spmns',
        confirmMsg: 'container.transfer_spmns_warn',
        isWarning: true,
        input: { storedSpmns: storedSpmns }
      }).then(
        function() { return specimens; },
        function() { return undefined; }
      );
    }

    return {
      fromOrdinal: NumberConverterUtil.fromNumber,

      toNumber: NumberConverterUtil.toNumber,

      getOpts: getOpts,

      assignPositions: function(container, occupancyMap, inputLabels, userOpts) {
        userOpts = userOpts || {};

        var opts = getOpts(container, false, false, userOpts.useBarcode);
        opts.occupants = occupancyMap;

        var result = BoxLayoutUtil.assignCells(opts, inputLabels, userOpts.vacateOccupants);
        return {map: result.occupants, noFreeLocs: result.noFreeLocs};
      },

      getSpecimens: getSpecimens
    };
  });
