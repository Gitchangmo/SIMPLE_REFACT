import os
import firebase_admin
from firebase_admin import credentials, firestore
from dotenv import load_dotenv

load_dotenv()

cred = credentials.Certificate(os.getenv("FIREBASE_CRED_PATH_SERVER", "./json_file/simple.json"))
firebase_admin.initialize_app(cred)
db = firestore.client()
