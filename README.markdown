## 9Cards Backend

### Summary

  This backend app contains all our public work. It's splitted in 3 different modules:

    * app: Contains all the stuff related to the app, collections, apps, settings, user config, ...
    * user: Contains all the stuff related to sign in & log in
    * api: Contains all the public api but no biz logic. It's going to work as a composition layer.

  Current endpoints that will be placed in this app are:

    ```
    GET           /api/ninecards/userconfig
    PUT           /api/ninecards/userconfig/checkpoint/purchase/:product
    PUT           /api/ninecards/userconfig/checkpoint/collection
    PUT           /api/ninecards/userconfig/checkpoint/joined/:otherConfigId
    PUT           /api/ninecards/userconfig/geoInfo
    PUT           /api/ninecards/userconfig/device
    PUT           /api/ninecards/userconfig/theme
    DELETE        /api/ninecards/userconfig/device/:deviceId
    PUT           /api/ninecards/userconfig/tester

    GET           /api/ninecards/collections/items/sponsored
    GET           /api/ninecards/collections/search/:keywords/:offset/:limit
    GET           /api/ninecards/collections/:filter/:category/:offset/:limit
    GET           /api/ninecards/collections/:filter/:offset/:limit
    POST          /api/ninecards/collections
    PUT           /api/ninecards/collections/:sharedCollectionId/subscribe
    DELETE        /api/ninecards/collections/:sharedCollectionId/subscribe
    POST          /api/ninecards/collections/:sharedCollectionId/rate/:stars
    POST          /api/ninecards/collections/:sharedCollectionId/notifyPlusOne
    POST          /api/ninecards/collections/:sharedCollectionId/notifyViews
    POST          /api/ninecards/collections/:sharedCollectionId/notifyInstall
    GET           /api/ninecards/collections/:sharedCollectionId
    ```

### Technologies to use

  The backend will be implemented over Spray and MongoBD(or Cassandra, it has to be discussed).
