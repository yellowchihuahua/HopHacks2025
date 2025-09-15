# HopHacks2025 - JauneGone

Submitted to [HopHacks2025](https://hophacks-fall-2025.devpost.com/), an in-person hackathon hosted by Johns Hopkins University from September 12 to 14, 2025. 

Devpost link: https://devpost.com/software/jaunegone

2nd place winner for Hardware + AR Track: AI + AR for Maternal Wellness by Orcava

## Inspiration
60% of newborns and 80% of premature babies develop jaundice. Parents often miss the early signs, and if left untreated, severe jaundice can lead to serious complications such as kernicterus (brain damage). Because this is such a widespread problem, we wanted to provide parents with a quick way to check whether their baby is showing symptoms of jaundice. We hope to improve quality of life and peace of mind by allowing parents to verify the health of their newborns from the safety of their own home.

## What it does
The user takes (or uploads) three pictures of their newborn's body, eyes, and feet. The app will analyze the picture of the baby's body and provide a judgment on whether the baby is likely to have jaundice, as well as appropriate next steps to seek a confirmatory diagnosis and treatment. This enables parents to perform a preliminary screening for jaundice symptoms from the comfort of their own homes. Our application is completely offline, the data processing is ephemeral and the lightweight model can run right on your phone in milliseconds. This ensures data security and allows the app to be accessible to less powerful hardware and places with no internet infrastructure, like regions of conflict or less developed parts of the world. 

## How we built it
Our app uses our AI model (JaundAIce) to analyze photos of newborns for signs of jaundice. We custom-trained JaundAIce using the Kaggle newborn jaundice dataset (at https://www.kaggle.com/datasets/aiolapo/jaundice-image-data/). Starting with MobileNetV2, a lightweight pre-trained CNN, we unfroze the last layers to account for our project-specific features, utilizing Python to train a classifier-based Keras model, which we then converted to Tensorflow Lite to be used with our Android app. The UI was built in Android Studio using Java.

## What's next for JauneGone
- Train the AI model on other jaundice markers, such as eyes and feet
- increase training dataset size to train a more accurate model
- Increase training dataset to account for darker skin tones
- Integrate healthcare access point of contacts - virtual doctors' appointments or nearby hospitals
- Potentially reach out for feedback/user-requested features (that fall within the app’s original scope)
