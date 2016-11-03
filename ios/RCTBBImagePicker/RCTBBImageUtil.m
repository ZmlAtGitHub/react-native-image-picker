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

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(scaleImage:(NSURLRequest *)request options:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    CGFloat width = [RCTConvert CGFloat:options[@"width"]];
    CGFloat height = [RCTConvert CGFloat:options[@"height"]];
    RCTResizeMode resizeMode = [RCTConvert RCTResizeMode:options[@"resizeMode"]];
    NSString *type = [RCTConvert NSString:@"type"];
    CGFloat quality = [RCTConvert CGFloat:@"quality"];
    
    [_bridge.imageLoader loadImageWithURLRequest:request size:CGSizeMake(width, height) scale:1.0 clipped:NO resizeMode:resizeMode progressBlock:^(int64_t progress, int64_t total) {
    } partialLoadBlock:^(UIImage *image) {
    } completionBlock:^(NSError *error, UIImage *image) {
    }];
    [_bridge.imageLoader loadImageWithURLRequest:[NSURLRequest requestWithURL:nil] callback:^(NSError *error, UIImage *image) {
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

RCT_EXPORT_METHOD(fixedOrientationOfImage:(NSURLRequest *)request resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    [_bridge.imageLoader loadImageWithURLRequest:request callback:^(NSError *error, UIImage *image) {
        if (error) {
            reject([NSString stringWithFormat:@"%ld", error.code], error.description, error);
        }
        else {
            image = [RCTBBImageUtil imageOfFixedOrientation:image];
            [_bridge.imageStoreManager storeImage:image withBlock:^(NSString *tempImageTag) {
                if (tempImageTag) {
                    resolve(tempImageTag);
                }
                else {
                    reject(@"-1", @"save error", nil);
                }
            }];
        }
    }];
}

+ (UIImage *)imageOfFixedOrientation:(UIImage *)aImage
{
    // No-op if the orientation is already correct
    if (aImage.imageOrientation == UIImageOrientationUp)
        return aImage;
    
    // We need to calculate the proper transformation to make the image upright.
    // We do it in 2 steps: Rotate if Left/Right/Down, and then flip if Mirrored.
    CGAffineTransform transform = CGAffineTransformIdentity;
    
    switch (aImage.imageOrientation) {
        case UIImageOrientationDown:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, aImage.size.width, aImage.size.height);
            transform = CGAffineTransformRotate(transform, M_PI);
            break;
            
        case UIImageOrientationLeft:
        case UIImageOrientationLeftMirrored:
            transform = CGAffineTransformTranslate(transform, aImage.size.width, 0);
            transform = CGAffineTransformRotate(transform, M_PI_2);
            break;
            
        case UIImageOrientationRight:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, 0, aImage.size.height);
            transform = CGAffineTransformRotate(transform, -M_PI_2);
            break;
        default:
            break;
    }
    
    switch (aImage.imageOrientation) {
        case UIImageOrientationUpMirrored:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, aImage.size.width, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;
            
        case UIImageOrientationLeftMirrored:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, aImage.size.height, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;
        default:
            break;
    }
    
    // Now we draw the underlying CGImage into a new context, applying the transform
    // calculated above.
    CGContextRef ctx = CGBitmapContextCreate(NULL, aImage.size.width, aImage.size.height,
                                             CGImageGetBitsPerComponent(aImage.CGImage), 0,
                                             CGImageGetColorSpace(aImage.CGImage),
                                             CGImageGetBitmapInfo(aImage.CGImage));
    CGContextConcatCTM(ctx, transform);
    switch (aImage.imageOrientation) {
        case UIImageOrientationLeft:
        case UIImageOrientationLeftMirrored:
        case UIImageOrientationRight:
        case UIImageOrientationRightMirrored:
            // Grr...
            CGContextDrawImage(ctx, CGRectMake(0,0,aImage.size.height,aImage.size.width), aImage.CGImage);
            break;
            
        default:
            CGContextDrawImage(ctx, CGRectMake(0,0,aImage.size.width,aImage.size.height), aImage.CGImage);
            break;
    }
    
    // And now we just create a new UIImage from the drawing context
    CGImageRef cgimg = CGBitmapContextCreateImage(ctx);
    UIImage *img = [UIImage imageWithCGImage:cgimg];
    CGContextRelease(ctx);
    CGImageRelease(cgimg);
    return img;
}

@end
