# Class : MainMenuActivity
|   Responsibilities   |   Collaborators   |
| ------------- |-------------| 
|  Display main menu| EventActivity |
| Have options for user to navigate to different sections (activities)  |  QRcodeActivity |
||ProfileActivity|
||EventActivity|

# Class : Event
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Store and manage information about an invent, including poster image, and status of entrants, aswell as requirements (Geolocation etc..) | FirestoreDatabase |
|Be able to be changed or modified by the Event organizer.|User|
|Hold or access a ‘pool’ of entrants to be drawn.|PoolingSystem|
||Geolocation|

# Class : FirestoreDatabase
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Connect to database | Event |
||User|

# Class : User
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Create Events with specifications for others to apply to.  | Event  |
|Apply to Events and be notified of decision (in progress, waiting list, accepted.)||
|Be able to access related abilities for each user type, as an entrant, organizer, or admin.||

# Class : PoolingSystem
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Contain list of applicants for an event | Event |
|Have Users able to remove themselves from the pool| User|
|Draw an User to be returned for an Event, and removed from the pool depending on specifications (limit on entrees?)||

# Class : QRcode
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Scan and access the correct events details from proper role of user (whether user is an entrant or organizer) | Event |
||User|
||QRcodeActivity|

# Class : Geolocation
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Contain data of a location and other parameters  | Event |
|Inform users that are entrants whether they can meet this requirement for an event or not.|User|

# Class : EventActivity
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| A screen where User can interact with  event(s). | User |
|Allow an organiser User to input data to create/update events with specifications||
|Allow user to return to Main menu|PoolingSystem|
||MainMenuActivity|

# Class : QRcodeActivity
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
| Display a view for scanning a QR code  | QRcode |
||Event|

# Class : ProfileActivity
|   Responsibility    |   Collaborators   |
| ------------- |-------------| 
|   User can view  a profile that contains information about the applicant.| User |









