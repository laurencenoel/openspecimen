
angular.module('os.biospecimen.cp.detail', ['os.biospecimen.models'])
  .controller('CpDetailCtrl', function(
    $scope, $q, $translate, cp,
    CollectionProtocol, PvManager, DeleteUtil, CpSettingsReg, SettingUtil) {

    function init() {
      //
      // SOP document names are of form: <cp id>_<actual filename>
      //
	  
      if (!!cp.sopDocumentName) {
        cp.$$sopDocDispName = cp.sopDocumentName.substring(cp.sopDocumentName.indexOf("_") + 1);
      } 

      $scope.cp = cp;
      $scope.cp.repositoryNames = cp.getRepositoryNames();
      $scope.downloadUri = CollectionProtocol.url() + cp.id + '/definition';
      $scope.workflowUri = CollectionProtocol.url() + cp.id + '/workflows-file';
      $scope.sites = PvManager.getSites();
      $scope.sysStoreSpr  = true;

      var opts = {sites: cp.repositoryNames, cp: cp.shortTitle};
      angular.extend($scope.cpResource.updateOpts, opts);
      angular.extend($scope.cpResource.deleteOpts, opts);

      CpSettingsReg.getSettings().then(
        function(settings) {
          $scope.settings = settings;
        }
      );

      SettingUtil.getSetting('biospecimen', 'store_spr').then(
        function(setting) {
          $scope.sysStoreSpr = (setting.value == 'true');
        }
      );
    }

    $scope.editCp = function(property, value) {
      var d = $q.defer();
      d.resolve({});
      return d.promise;
    }

    $scope.deleteCp = function() {
      DeleteUtil.delete($scope.cp, {onDeleteState: 'cp-list', forceDelete: true, askReason: true});
    }

    init();
  });
