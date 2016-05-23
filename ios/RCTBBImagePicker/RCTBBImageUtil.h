//
//  RCTBBImageUtil.h
//  RCTBBImagePicker
//
//  Created by lvbingru on 16/4/20.
//  Copyright © 2016年 erica. All rights reserved.
//

#import "RCTBridgeModule.h"
#import <UIKit/UIKit.h>

@interface RCTBBImageUtil : NSObject<RCTBridgeModule>

+ (UIImage *)imageOfFixedOrientation:(UIImage *)aImage;

@end
