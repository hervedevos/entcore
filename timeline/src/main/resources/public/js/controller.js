function MainController($rootScope, $scope){
	$scope.closePanel = function(){
		$rootScope.$broadcast('close-panel');
	}
}

function Timeline($scope, date, model, lang){
	$scope.notifications = model.notifications;
	$scope.notificationTypes = model.notificationTypes
	$scope.translate = lang.translate;

	model.on('notifications.change, notificationTypes.change', function(e){
		if(!$scope.$$phase){
			$scope.$apply('notifications');
			$scope.$apply('notificationTypes');
		}
	})

	$scope.formatDate = function(dateString){
		return date.calendar(dateString);
	};

	$scope.removeFilter = function(){
		Model.notificationTypes.removeFilter();
	}
}

function Personalization($rootScope, $scope, model, ui){
	$scope.skins = model.skins;

	$scope.saveTheme = function(skin, $event){
		$event.stopPropagation();
		skin.setForUser();
		ui.setStyle(skin.skinPath);
	};

	$scope.togglePanel = function($event){
		$scope.showPanel = !$scope.showPanel;
		$event.stopPropagation();
	};

	$rootScope.$on('close-panel', function(e){
		$scope.showPanel = false;
	})
}

function Widgets($scope, model, lang){
	$scope.widgets = model.widgets;

	model.on('widgets.change', function(){
		if(!$scope.$$phase){
			$scope.$apply('widgets');
		}
	})

	$scope.translate = lang.translate;
}