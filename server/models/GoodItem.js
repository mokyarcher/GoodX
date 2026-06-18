const mongoose = require('mongoose');

const goodItemSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true,
    maxlength: 100
  },
  description: {
    type: String,
    trim: true,
    maxlength: 1000
  },
  contentType: {
    type: String,
    enum: ['goods', 'moments', 'entertainment'],
    default: 'goods'
  },
  category: {
    type: String,
    required: true,
    enum: [
      'electronics', 'lifestyle', 'fashion', 'software', 'subscription', 'other_goods',
      'scenery', 'city', 'travel', 'daily', 'place', 'other_moments',
      'movie', 'series', 'music', 'book', 'game', 'anime', 'podcast', 'other_entertainment'
    ]
  },
  subCategory: {
    type: String,
    trim: true
  },
  status: {
    type: String,
    enum: ['active', 'removed', 'pending_review'],
    default: 'active'
  },
  removedSnapshot: {
    type: String,
    default: null
  },
  removeReason: {
    type: String,
    default: null
  },
  images: [{
    type: String
  }],
  link: {
    type: String,
    trim: true
  },
  author: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  likes: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  comments: [{
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    content: {
      type: String,
      required: true
    },
    likes: [{
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    }],
    parentId: {
      type: mongoose.Schema.Types.ObjectId,
      default: null
    },
    createdAt: {
      type: Date,
      default: Date.now
    }
  }]
}, {
  timestamps: true
});

module.exports = mongoose.model('GoodItem', goodItemSchema);
