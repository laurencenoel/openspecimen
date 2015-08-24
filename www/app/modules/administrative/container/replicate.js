
angular.module('os.administrative.container.replicate', ['os.administrative.models'])
  .controller('ContainerReplCtrl', function($scope, $modal, container, Site, Container, Alerts) {
    var siteContainersMap = {};

    var containerDetailMap = {};

    function init() {
      $scope.repl = {countView: true, count: 1};

      $scope.container = container;
      $scope.sites = [];
      $scope.destinations = [];
    }

    function loadSites() {
      Site.listForContainers('Create').then(
        function(sites) {
          $scope.sites = sites;
        }
      );
    }

    function addDest() {
      $scope.destinations.push({
        name: '',
        siteName: container.siteName, 
        parentContainerId: container.parentContainerId,
        posOne: '', 
        posTwo: ''
      });
    }

    function setSiteContainers(dest) {
      if (!siteContainersMap[dest.siteName]) {
        siteContainersMap[dest.siteName] = Container.listForSite(dest.siteName, true, true);
      }

      siteContainersMap[dest.siteName].then(
        function(containers) {
          dest.containers = containers;
        }
      );
    }

    $scope.createReplicaHolders = function() {
      loadSites();

      for (var i = 0; i < $scope.repl.count; ++i) {
        addDest();
      }

      $scope.repl.countView = false;
    }

    $scope.onSiteSelect = function(dest) {
      setSiteContainers(dest);
      dest.parentContainerId = dest.posOne = dest.posTwo = undefined;
    }

    $scope.searchContainers = function(name, dest) {
      if (!name) {
        setSiteContainers(dest);
      } else {
        Container.listForSite(dest.siteName, true, true, name).then(
          function(containers) {
            dest.containers = containers;
          }
        );
      }
    }

    $scope.onContainerSelect = function(dest) {
      dest.posOne = dest.posTwo = undefined;
    }

    $scope.openPositionSelector = function(dest) {
      var modalInstance = $modal.open({
        templateUrl: 'modules/administrative/container/position-selector.html',
        controller: function($scope, $modalInstance, container) {
          $scope.container = container;
          $scope.position = {positionX: '', positionY: ''};

          $scope.ok = function() {
            $modalInstance.close($scope.position);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        },
        size: 'lg',
        resolve: {
          container: function() {
            if (!containerDetailMap[dest.parentContainerId]) {
              containerDetailMap[dest.parentContainerId] = Container.getById(dest.parentContainerId);
            }

            return containerDetailMap[dest.parentContainerId];
          }
        }
      });

      modalInstance.result.then(
        function(position) {
          dest.posOne = position.positionX;
          dest.posTwo = position.positionY;
        }
      );
    };

    $scope.addDest = addDest;

    $scope.removeDest = function(idx) {
      $scope.destinations.splice(idx, 1);
    }

    $scope.replicate = function() {
      var destinations = [];
      for (var i = 0; i < $scope.destinations.length; ++i) {
        var uiDest = $scope.destinations[i];
        destinations.push({
          name: uiDest.name,
          siteName: uiDest.siteName, 
          parentContainerId: uiDest.parentContainerId,
          posOne: uiDest.posOne,
          posTwo: uiDest.posTwo
        });
      }
            
      $scope.container.replicate(destinations).then(
        function() {
          Alerts.success('container.replicated_successfully', $scope.container);
          $scope.back();
        }
      );
    }

    $scope.cancel = function() {
      $scope.back();
    }

    init();
  });