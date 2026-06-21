const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  username: {
    type: String,
    required: true,
    unique: true,
    trim: true,
    minlength: 3,
    maxlength: 20
  },
  password: {
    type: String,
    required: true,
    minlength: 6
  },
  nickname: {
    type: String,
    trim: true,
    maxlength: 30
  },
  avatar: {
    type: String,
    default: null
  },
  isAdmin: {
    type: Boolean,
    default: false
  },
  banned: {
    type: Boolean,
    default: false
  },
  inviteCode: {
    type: String,
    unique: true,
    sparse: true
  },
  circles: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Circle'
  }],
  favorites: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'GoodItem'
  }]
}, {
  timestamps: true
});

module.exports = mongoose.model('User', userSchema);
