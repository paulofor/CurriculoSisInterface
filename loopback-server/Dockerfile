FROM node:10-buster

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 25012
CMD ["npm", "start"]
