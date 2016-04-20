//
//  RCTBBImageUtil.m
//  RCTBBImagePicker
//
//  Created by lvbingru on 16/4/20.
//  Copyright © 2016年 erica. All rights reserved.
//

#import "RCTBBImageUtil.h"
#import "RCTImageStoreManager.h"
#import "RCTImageLoader.h"
#import "RCTResizeMode.h"

@implementation RCTBBImageUtil

@synthesize bridge = _bridge;

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(scaleImage:(NSString *)imageTag options:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    CGFloat width = [RCTConvert CGFloat:options[@"width"]];
    CGFloat height = [RCTConvert CGFloat:options[@"height"]];
    RCTResizeMode resizeMode = [RCTConvert RCTResizeMode:options[@"resizeMode"]];
    NSString *type = [RCTConvert NSString:@"type"];
    CGFloat quality = [RCTConvert CGFloat:@"quality"];
    
    [_bridge.imageLoader loadImageWithTag:imageTag size:CGSizeMake(width, height) scale:1.0 resizeMode:resizeMode progressBlock:NULL completionBlock:^(NSError *error, UIImage *image) {
        if (error) {
            reject([NSString stringWithFormat:@"%ld", error.code], error.description, error);
        }
        else {
            NSData *imageData = nil;
            if ([type isEqualToString:@"png"]) {
                imageData = UIImagePNGRepresentation(image);
            }
            else {
                imageData = UIImageJPEGRepresentation(image, !!quality || 1.0);
            }
            NSString *base64String = [imageData base64EncodedStringWithOptions:0];
            resolve(base64String);
        }
    }];
}

@end
