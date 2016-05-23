/**
 * Created by lvbingru on 2015-12-29.
 */

import {NativeModules} from 'react-native';

import SystemImagePicker from './SystemImagePicker';
import ImageAddBlock from './js/ImageAddBlock';
import ImagePicker from './js/ImagePicker';

const fixImageOrientation =  NativeModules.BBImageUtil && NativeModules.BBImageUtil.fixedOrientationOfImage;

module.exports = {
  SystemImagePicker,
  ImageAddBlock,
  ImagePicker,
  fixImageOrientation,
};