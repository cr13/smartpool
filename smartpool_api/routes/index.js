var express = require('express');
var router = express.Router();
var admin = require("firebase-admin");

var serviceAccount = require("../smartpoolXXXXXXXXXXXXXXXXXXXXXXX.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://smartpoolXXXXXXXXXX.firebaseio.com/"
});

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Express' });
});

router.post('/sendData', function(req, res) {
	console.log("Salvando datos")
	var json = req.body.data.replace(/\'/g,'"')
	json = JSON.parse(json);
	 console.log(json)
	admin.auth().getUser(req.body.uid)
	  .then(function(userRecord) {
		var db = admin.database();
		var ref = db.ref("Devices").child(req.body.mac).child(req.body.fecha).getRef();	
		
		for (var i in json){
			ref.child(i).child("valor").set(json[i].value);
			ref.child(i).child("nombre").set(json[i].name);
			ref.child(i).child("unit").set(json[i].unit);
		}
		
		
		 // console.log("Successfully fetched user data:", userRecord.toJSON());
		res.status(200).send('Actualizado');

	  })
	  .catch(function(error) {
		
		res.status(500).send('Error al actualizar');

	  });
});

router.post('/sendDataJ', function(req, res) {
	console.log("Salvando datos desde JSON")
	var json = req.body.data.replace(/\'/g,'"')
	json = JSON.parse(json);
	 console.log(json)
	admin.auth().getUser(req.body.uid)
	  .then(function(userRecord) {
		var db = admin.database();
		var ref;
		console.log("Logeado con exito");

		for (var i in json){
			ref = db.ref("Devices").child(i).getRef();
			console.log(i);
			for (let j in json[i]) {
				for (let k in json[i][j]) {
					console.log("aqui"+k);
					ref.child(j).child(k).child("valor").set(json[i][j][k].value);
					ref.child(j).child(k).child("nombre").set(json[i][j][k].name);
					ref.child(j).child(k).child("unit").set(json[i][j][k].unit);
					
				}
			}
		}
		
		 // console.log("Successfully fetched user data:", userRecord.toJSON());
		res.status(200).send('Actualizado');

	  })
	  .catch(function(error) {
		
		res.status(500).send('Error al actualizar');

	  });
});

module.exports = router;
