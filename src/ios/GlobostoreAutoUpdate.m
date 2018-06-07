#import "GlobostoreAutoUpdate.h"
#import <UIKit/UIKit.h>

@implementation GlobostoreAutoUpdate

UIViewController *viewTemplate;

- (void) pluginInitialize{
}

- (void)check:(CDVInvokedUrlCommand *) command {
    
    NSString *splashDurationString = [self.commandDelegate.settings objectForKey: [@"versionUrl" lowercaseString]];
    
    [self validateCurrentVersion:nil];
} 

-(void)placePostRequestWithURL:(NSString *)action withData:(NSDictionary *)dataToSend withHandler:(void (^)(NSURLResponse *response, NSData *data, NSError *error))ourBlock {
    
    NSString *appKey = [self.commandDelegate.settings objectForKey: [@"appKey" lowercaseString]];
    
    NSString *urlString = [NSString stringWithFormat:@"%@", action];
    NSLog(@"%@", urlString);
    
    NSURL *url = [NSURL URLWithString:urlString];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    
    NSError *error;
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dataToSend options:0 error:&error];
    
    NSString *jsonString;
    if (! jsonData) {
        NSLog(@"Got an error: %@", error);
    } else {
        jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        
        NSData *requestData = [NSData dataWithBytes:[jsonString UTF8String] length:[jsonString lengthOfBytesUsingEncoding:NSUTF8StringEncoding]];
        
        [request setHTTPMethod:@"POST"];
        [request setValue:@"application/json" forHTTPHeaderField:@"Accept"];
        [request setValue:@"application/json; charset=UTF-8" forHTTPHeaderField:@"Content-Type"];
        [request setValue:appKey forHTTPHeaderField:@"appkey"];
        [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)[requestData length]] forHTTPHeaderField:@"Content-Length"];
        [request setHTTPBody: requestData];
        
        [NSURLConnection sendAsynchronousRequest:request queue:[NSOperationQueue mainQueue] completionHandler:ourBlock];
    }
}

- (void) api:(NSDictionary *)data
    calledBy:(id)calledBy
 withSuccess:(SEL)successCallback
  andFailure:(SEL)failureCallback {
    
    NSString *versionUrl = [self.commandDelegate.settings objectForKey: [@"versionUrl" lowercaseString]];
    
    [self placePostRequestWithURL:versionUrl
                         withData:data
                      withHandler:^(NSURLResponse *response, NSData *rawData, NSError *error) {
                          NSString *string = [[NSString alloc] initWithData:rawData
                                                                   encoding:NSUTF8StringEncoding];
                          
                          NSHTTPURLResponse* httpResponse = (NSHTTPURLResponse*)response;
                          NSInteger code = [httpResponse statusCode];
                          NSLog(@"%ld", (long)code);
                          
                          if (!(code >= 200 && code < 300)) {
                              NSLog(@"ERROR (%ld): %@", (long)code, string);
                              [calledBy performSelector:failureCallback withObject:string];
                          } else {
                              NSLog(@"OK");
                              [calledBy performSelector:successCallback withObject:rawData];
                          }
                      }];
}

- (void) validateCurrentVersion: (UIViewController *)view {
    
    NSString *appId = [self.commandDelegate.settings objectForKey: [@"appId" lowercaseString]];
    NSString *appOs = @"ios";
    NSString *method = [self.commandDelegate.settings objectForKey: [@"method" lowercaseString]];
    NSString *service = [self.commandDelegate.settings objectForKey: [@"service" lowercaseString]];
    
    NSMutableDictionary *dataToSend=[NSMutableDictionary dictionaryWithDictionary:@{@"request" :@{@"app_id" :appId,@"os":appOs}, @"method": method, @"service": service}];
    
    [self api:dataToSend
     calledBy:self
  withSuccess:@selector(apiDidEnd:)
   andFailure:@selector(apiFailure:)];
    
}

- (void)apiDidEnd:(id)result{
    
    NSLog(@"apiupdateDidEnd:");
    NSError* error;
    NSDictionary* responseDataDict = [NSJSONSerialization JSONObjectWithData:result
                                                                     options:kNilOptions
                                                                       error:&error];
    
    NSString *remoteVersion = responseDataDict[@"version"][@"version"];
    NSString *path = responseDataDict[@"version"][@"download"];
    
    NSString *downloadUrl = [self.commandDelegate.settings objectForKey: [@"downloadUrl" lowercaseString]];
    
    NSString *prepareToUpload = [self.commandDelegate.settings objectForKey: [@"prepareToUpload" lowercaseString]];
    
    NSString *bundleVersion = [NSString stringWithFormat:@"%@",[[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"]];
    
    if (![bundleVersion isEqualToString:remoteVersion] && !prepareToUpload) {

        [self.commandDelegate.settings setValue:[NSNumber numberWithBool: NO] forKey:@"prepareToUpload"];
        
        NSString *itms = @"itms-services://?action=download-manifest&url=";
        NSString *origin = [NSString stringWithFormat: @"%@%@/", itms, downloadUrl];
        NSString *downloadPath = [NSString stringWithFormat: @"%@%@", origin, path];
        
        NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
        [prefs setObject:downloadPath forKey:@"downloadPath"];
        
        [self.commandDelegate.settings setValue:downloadPath forKey:@"downloadPath"];
        [self.commandDelegate.settings setValue:remoteVersion forKey:@"remoteVersion"];
        
        NSURL *link = [NSURL URLWithString:downloadPath];
        
        NSString *messageTeste = [NSString stringWithFormat: @"%@ corrente: %@ disponível: %@", @"Nova versão disponível", bundleVersion, remoteVersion];

        UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Atualização" message:messageTeste
                                    
                                    
                                                                preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction* defaultAction = [UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault
                                                              handler:^(UIAlertAction * action) {
                                                                  [self handleAlert:link];
                                                              }];
        
        [alert addAction:defaultAction];
        [[self currentView:viewTemplate] presentViewController:alert animated:YES completion:nil];
        
    }
    
}

- (void)apiFailure:(id)result{
    NSLog(@"apiUpdateFailure:");
}


- (void) handleAlert:(NSURL *)downloadLink {
    BOOL success = [[UIApplication sharedApplication] openURL:downloadLink];
    if (success) {
        NSLog(@"Opened url");
        [self.commandDelegate.settings setValue:[NSNumber numberWithBool: YES] forKey:@"prepareToUpload"];
    }
}

- (UIViewController*) topMostController {
    UIViewController *topController = [UIApplication sharedApplication].keyWindow.rootViewController;
    while (topController.presentedViewController) {
        topController = topController.presentedViewController;
    }
    return topController;
}

- (UIViewController*) currentView: (UIViewController *)view {
    if (view == nil) {
         return [self topMostController];
    } else {
        return view;
    }
}




@end
