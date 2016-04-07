//
//  RCTBBImagePicker.m
//  RCTBBImagePicker
//
//  Created by LvBingru on 3/16/16.
//  Copyright © 2016 erica. All rights reserved.
//

#import "RCTBBImagePicker.h"
#import <UIKit/UIKit.h>
#import <MobileCoreServices/UTCoreTypes.h>
#import "RCTUtils.h"
#import "RCTConvert.h"

#import "RCTAssetsLibraryRequestHandler.h"
#import <AssetsLibrary/AssetsLibrary.h>
#import "RCTImageStoreManager.h"
#import "RCTCameraRollManager.h"

@interface RCTBBImagePicker()<UINavigationControllerDelegate, UIImagePickerControllerDelegate>

@property (nonatomic, copy) RCTResponseSenderBlock resolveBlock;
@property (nonatomic, copy) RCTResponseSenderBlock rejectBlock;
@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, assign) BOOL savePhoto;

@end

@implementation RCTBBImagePicker

RCT_EXPORT_MODULE(BBImagePicker);

@synthesize bridge = _bridge;

- (NSDictionary *)constantsToExport
{
    return @{ @"canRecordVideos": @([[UIImagePickerController availableMediaTypesForSourceType:UIImagePickerControllerSourceTypeCamera] containsObject:(NSString *)kUTTypeMovie]),
              @"canUseCamera": @([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera])
              };
}

- (void)dealloc
{
    self.resolveBlock = nil;
    self.rejectBlock = nil;
    self.picker.delegate = nil;
    
    [self.picker dismissViewControllerAnimated:NO completion:NULL];
    
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(getAlumGroupNames:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    ALAssetsGroupType groupTypes = [RCTConvert ALAssetsGroupType:params[@"groupTypes"]];

    NSMutableArray *groupNames = [NSMutableArray new];
    [_bridge.assetsLibrary enumerateGroupsWithTypes:groupTypes usingBlock:^(ALAssetsGroup *group, BOOL *stopGroups) {
        if (group) {
            [groupNames addObject:[group valueForProperty:ALAssetsGroupPropertyName]];
        }
        else {
            resolve(groupNames);
        }
    } failureBlock:^(NSError *error) {
        reject([NSString stringWithFormat:@"%d",error.code], nil, error);
    }];

}

RCT_EXPORT_METHOD(openCamera:(NSDictionary *)options resolve:(RCTResponseSenderBlock)resolve reject:(RCTResponseSenderBlock)reject)
{
    if (RCTRunningInAppExtension()) {
        reject(@[@"Image picker is currently unavailable in an app extension"]);
        return;
    }
    
    if (self.picker) {
        [self.picker dismissViewControllerAnimated:NO completion:NULL];
        self.picker = nil;
    }
    
    self.resolveBlock = resolve;
    self.rejectBlock = reject;
    
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    
    NSString *sourceType = [RCTConvert NSString:options[@"sourceType"]];
    
    if ([sourceType isEqualToString:@"library"]) {
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }
    else if ([sourceType isEqualToString:@"album"]){
        picker.sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
    }
    else {
        picker.sourceType = UIImagePickerControllerSourceTypeCamera;
        
        self.savePhoto = [RCTConvert BOOL:options[@"savePhoto"]];
    }
    
    if ([RCTConvert BOOL:options[@"videoMode"]]) {
        picker.mediaTypes = @[(NSString*)kUTTypeVideo,(NSString*)kUTTypeMovie];
        if (picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
            picker.cameraCaptureMode = UIImagePickerControllerCameraCaptureModeVideo;
            
            if (options[@"videoQuality"]) {
                NSInteger videoQuality = [RCTConvert NSInteger:options[@"videoQuality"]];
                picker.videoQuality = videoQuality;
            }
            
            if (options[@"videoMaximumDuration"]) {
                NSInteger videoMaximumDuration = [RCTConvert NSInteger:options[@"videoMaximumDuration"]];
                picker.videoMaximumDuration = videoMaximumDuration;
            }
        }
    }
    
    if ([RCTConvert BOOL:options[@"allowsEditing"]]) {
        picker.allowsEditing = true;
    }
    
    picker.delegate = self;
    self.picker = picker;
    
    UIViewController *controller = RCTKeyWindow().rootViewController;
    while (controller.presentedViewController) {
        controller = controller.presentedViewController;
    }
    [controller presentViewController:picker animated:YES completion:nil];
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    self.picker = nil;
    
    if (picker.sourceType == UIImagePickerControllerSourceTypeCamera && picker.cameraCaptureMode == UIImagePickerControllerCameraCaptureModeVideo) {
        NSString *videoUri = [info[UIImagePickerControllerMediaURL] absoluteString];
        [self _dismissPicker:picker args:@[videoUri]];
    }
    else {
        NSString *imageUri = [info[UIImagePickerControllerReferenceURL] absoluteString];
        if (imageUri) {
            [self _dismissPicker:picker args:@[imageUri]];
        } else {
            UIImage *image;
            if (picker.allowsEditing) {
                image = [info objectForKey:UIImagePickerControllerEditedImage];
            }
            else {
                image = [info objectForKey:UIImagePickerControllerOriginalImage];
            }
            
            if (self.savePhoto) {
                [_bridge.assetsLibrary writeImageToSavedPhotosAlbum:image.CGImage metadata:nil completionBlock:^(NSURL *assetURL, NSError *saveError) {
                    if(saveError){
                        [self _dismissPicker:picker args:nil];
                    }
                    else {
                        [self _dismissPicker:picker args:@[assetURL.absoluteString]];
                    }
                }];
            }
            else {
                [_bridge.imageStoreManager storeImage:image withBlock:^(NSString *tempImageTag) {
                    if (tempImageTag) {
                        [self _dismissPicker:picker args:@[tempImageTag]];
                    }
                    else {
                        [self _dismissPicker:picker args:nil];
                    }
                }];
            }
        }
    }
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    self.picker = nil;
    
    [picker dismissViewControllerAnimated:YES completion:nil];
    
    if (self.rejectBlock) {
        self.rejectBlock(@[@"Cancelled"]);
    }
    
    self.resolveBlock = nil;
    self.rejectBlock = nil;
}

- (void)_dismissPicker:(UIImagePickerController *)picker args:(NSArray *)args
{
    [picker dismissViewControllerAnimated:YES completion:nil];
    
    if (args) {
        if (self.resolveBlock) {
            self.resolveBlock(args);
        }
    } else {
        if (self.rejectBlock) {
            self.rejectBlock(@[@"SaveError"]);
        }
    }
    
    self.resolveBlock = nil;
    self.rejectBlock = nil;
}

@end
