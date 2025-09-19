# 1. Database Setup

sql-- Create database

CREATE DATABASE feedback_bot;


# 2. Telegram Bot Setup

Message @BotFather on Telegram

Create new bot: /newbot 

Choose name and username

Copy the bot token

# 3. OpenAI Setup

Register at OpenAI

Create API key in your dashboard

Add billing information if needed


# 4. Google Docs Setup

1) Go to Google Cloud Console https://console.cloud.google.com/

2) Create new project or select existing

3) Enable Google Docs API

Create Service Account:


Go to IAM & Admin → Service Accounts

Create new service account

Download JSON credentials file


Create Google Document:

Create new Google Doc

Share with service account email (Editor access)

Copy document ID from URL


# 5. Trello Setup 

Get API key: https://trello.com/app-key

Generate token: https://trello.com/1/authorize?expiration=never&scope=read,write&response_type=token&name=FeedbackBot&key=YOUR_API_KEY

Get list ID:

Open Trello board
Add .json to URL to get board data
Find target list ID

# 6. Configuration
Update application.properties:
<img width="796" height="781" alt="image" src="https://github.com/user-attachments/assets/a6543cd4-0d5b-43b9-846b-11cada680c8b" />

# 7. Build and Run
use bash commands:

Clone repository

git clone https://github.com/Duskmage2009/feedback-bot

cd employee-feedback-bot


# Build
mvn clean compile

# Run
mvn spring-boot:run
# 8. Docker Setup (Alternative)
# Build image

docker build -t feedback-bot .

# Run with docker-compose
docker-compose up -d
# 9 Usage BOT
Bot Commands:

1. Start bot: /start

2. Select position: Mechanic/Electrician/Manager

3. Enter branch name

4. Send feedback messages anytime

Admin Panel

Access admin endpoints:

GET /api/admin/feedbacks - Get all feedbacks with filtering

GET /api/admin/statistics - Get statistics dashboard

GET /api/admin/critical-feedbacks - Get critical feedbacks only

GET /api/admin/branches - Get all branches


Example API calls:

 Get feedbacks with filters

curl "http://localhost:8080/api/admin/feedbacks?branch=Main&minCriticality=3&page=0&size=10"


 Get statistics
 
curl "http://localhost:8080/api/admin/statistics"
