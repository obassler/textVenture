
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import os

# Path to your Firebase service account key JSON file
# Make sure this file is in the same directory as this script, or provide the full path
SERVICE_ACCOUNT_KEY_PATH = 'textventure-1bb77-firebase-adminsdk-fbsvc-78b265eb3a.json'

# Initialize Firebase Admin SDK
if not firebase_admin._apps:
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase Admin SDK: {e}")
        print(f"Please ensure '{SERVICE_ACCOUNT_KEY_PATH}' is correctly placed and valid.")
        exit()

db = firestore.client()

def add_data_to_firestore():
    print("Populating Firestore with game data...")

    # --- Enemies Collection ---
    enemies_data = [
        {
            "id": "gruff_person",
            "name": "Gruff Person",
            "health": 50,
            "damage": 15,
            "description": "A burly individual with a menacing scowl."
        }
    ]

    for enemy in enemies_data:
        doc_ref = db.collection('enemies').document(enemy['id'])
        doc_ref.set(enemy)
        print(f"Added enemy: {enemy['name']}")

    # --- Locations Collection ---
    locations_data = [
        {
            "id": "start_location",
            "name": "Jail Room",
            "description": "You woke up in a cold, damp jail room. The air is stale, and a small, barred window offers a glimpse of a grey sky. What do you do?",
            "availableChoices": [
                {
                    "id": "search_surroundings",
                    "text": "Search your surroundings",
                    "effectType": "set_flag",
                    "flagToSet": {"foundKey": True},
                    "targetId": None,
                    "condition": None
                },
                {
                    "id": "unlock_jail",
                    "text": "Try to unlock the jail door",
                    "effectType": "move_location",
                    "targetId": "prison_yard",
                    "condition": {"flag": "foundKey", "value": True},
                    "flagToSet": {"jailUnlocked": True}
                }
            ],
            "itemsPresent": [],
            "enemyPresentId": None
        },
        {
            "id": "prison_yard",
            "name": "Prison Yard",
            "description": "The prison yard is empty, eerily quiet. You see a path leading out. Suddenly, a gruff figure blocks your way.",
            "availableChoices": [
                {
                    "id": "fight_person",
                    "text": "Confront the person",
                    "effectType": "start_combat",
                    "targetId": "gruff_person",
                    "condition": None
                }
            ],
            "itemsPresent": [],

            "enemyPresentId": "gruff_person"
        }
    ]

    for location in locations_data:
        doc_ref = db.collection('locations').document(location['id'])
        doc_ref.set(location)
        print(f"Added location: {location['name']}")

    print("Firestore population complete.")

if __name__ == '__main__':
    add_data_to_firestore()

