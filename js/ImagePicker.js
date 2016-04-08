/**
 * Created by lvbingru on 2/29/16.
 */

import {styles} from './ImagePicker.style.js'

import React, {InteractionManager, Component, PropTypes, View, Text, ListView, Image, Platform, TouchableOpacity, Alert, CameraRoll} from 'react-native';
import Icon from 'react-native-vector-icons/FontAwesome';
import SystemImagePicker from '../SystemImagePicker';
import semver from 'semver';
import packageData from 'react-native/package.json';

const propTypes = {
    limit : PropTypes.number,
    onSelectFinished : PropTypes.func,
    navigator : PropTypes.object,
    groupName : PropTypes.string,
}

const defaultProps = {
    limit: 65535,
    groupName : undefined
}

export default class ImagePicker extends Component {
    constructor(props) {
        super(props);

        this.imagesArray = [];
        this.selectedArray = [];

        this.state = {
            dataSource: new ListView.DataSource({rowHasChanged: (r1, r2) => (r1 !== r2 || r1.selected!==r2.selected)}).cloneWithRows([]),
        };
    }

    componentDidMount() {
        InteractionManager.runAfterInteractions(() => {
            this.fetchData();
        });
    }

    unSelectedAll() {
        this.imagesArray = this.imagesArray.map((el,index)=> {
            if(el.selected) {
                return {...el, selected: false}
            }
            else {
                return el
            }
        });
        this.selectedArray = [];
        this.updateDataSource(0);
    }

    render() {
        const {dataSource} = this.state;

        return (
            <View style={[styles.container]}>
                {
                  this.renderNavBar()
                }
                <ListView
                    renderHeader={this.renderHeader.bind(this)}
                    dataSource={dataSource}
                    renderRow={(rowData, sectionID, rowID) => {
                        return this.renderRow(rowData, parseInt(rowID)+4);
                     }}
                    horizontal = {false}
                    showsHorizontalScrollIndicator = {false}
                    removeClippedSubviews = {true}
                    contentContainerStyle = {[styles.listView]}
                    onEndReachedThreshold = {4000}
                    onEndReached = {()=>{
                        this.fetchData(true)
                    }}
                    pageSize={10}
                />
                <View style = {styles.pickerBottom}>
                    <TouchableOpacity
                      style = {[styles.bntBottom]}
                      onPress = {()=>{
                          this.unSelectedAll()
                      }}
                    >
                        <Text textStyle={styles.txtBottom}>取消</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style = {[styles.bntBottom]}
                        onPress = {this.onFinish.bind(this)}
                    >
                        {
                            !!this.selectedArray.length &&
                              <View style = {styles.viewNum}>
                                  <Text style = {styles.txtNum}>{this.selectedArray.length}</Text>
                              </View>
                        }
                        <Text style = {styles.txtBottom}>
                            确定
                        </Text>

                    </TouchableOpacity>
                </View>
            </View>
        );
    }

    renderNavBar() {
        return (
          <View style={styles.navBar}>
              <TouchableOpacity
                style={styles.navButton}
                onPress = {()=>this.props.navigator.pop()}
              >
                  <Text style = {styles.txtNavButton}>返回</Text>
              </TouchableOpacity>
              <View style={styles.navTitle}>
                  <Text style = {styles.txtNavTitle}>选择照片</Text>
              </View>
              <TouchableOpacity style={styles.navButton}>
                  <Text style = {styles.txtNavButton} />
              </TouchableOpacity>
          </View>
        )
    }

    renderHeader() {
        const num = Math.min(4,this.imagesArray.length);
        const list = this.imagesArray.slice(0, num);

        return (
          <View style = {styles.header}>
              <TouchableOpacity
                style = {styles.camera}
                onPress = {this.onCamera.bind(this)}
              >
                  <Icon
                    name = {'camera'}
                    style = {styles.fontCamera}
                  />
              </TouchableOpacity>
              <View style = {styles.headerItem}>
                  {
                      list.map((el,index)=>{
                          return this.renderRow(el, index);
                      })
                  }
              </View>
          </View>
        )
    }

    renderRow(rowData, index) {
        return (
          <TouchableOpacity
            key = {index}
            onPress = {()=>{
                this.onSelectedChanged(rowData, index)
            }}
            style = {styles.listViewItem}
          >
              <Image
                source = {{uri:this.getImage(rowData)}}
                style = {styles.img}
              />
              <Icon
                name="check-circle-o"
                style={styles.select}
                size={20}
                color={rowData.selected?'green':'#b4b7b9'}
              />
          </TouchableOpacity>
        )
    }

    fetchData(next) {
        if (next) {
        }
        else {
            this.imagesArray = []
        }

        const length = this.imagesArray.length;
        const lastObject = this.imagesArray[length-1];

        this.getPhotos({
            groupName : this.props.groupName,
              first: 100,
              after: (next && length > 0) ? this.getImage(lastObject) : undefined,
          },
          data=> {
              if (data.edges.length) {
                  data.edges.forEach(el=> {
                      this.imagesArray.push({...el, selected:false});
                  })

                  InteractionManager.runAfterInteractions(() => {
                      this.updateDataSource();
                  });
              }
          },
          e=> {
          }
        );
    }

    getPhotos(parmas, success, failed) {
        if (semver.gte(packageData.version, '0.20.0')) {
            CameraRoll.getPhotos(parmas).then(r=>{
                success(r);
            },e=>{
                failed(e);
            })
        }
        else {
            CameraRoll.getPhotos(params, success, failed);
        }
    }

    getImage(obj) {
        return obj.node.image.uri;
    }

    onSelectedChanged(rowData, rowIndex) {
        const {limit} = this.props;

        const should = rowData.selected || this.selectedArray.length < limit;
        if (!should) {
            Alert.alert("最多选择"+limit+"张");
            return false;
        }

        const uri = this.getImage(rowData);
        if (rowData.selected) {
            const index = this.selectedArray.indexOf(uri)
            this.selectedArray.splice(index,1);
        }
        else {
            this.selectedArray.push(uri);
        }

        this.imagesArray = this.imagesArray.map((el,index)=> {
            let object = el;
            if (index === rowIndex) {
                object = {...el,selected: !rowData.selected};
            }
            return object;
        })
        this.updateDataSource();

        return true
    }

    updateDataSource() {
        const ds = this.imagesArray.slice(4, this.imagesArray.length);
        this.setState({
            dataSource : this.state.dataSource.cloneWithRows(ds),
        });
    }

    onCamera() {
        const {onSelectFinished} = this.props;
        SystemImagePicker.openCameraDialog({sourceType:'camera'}).then(r=>{
            onSelectFinished && onSelectFinished([r]);
            this.props.navigator.pop();
        });
    }

    onFinish() {
        const {onSelectFinished} = this.props;
        onSelectFinished && onSelectFinished(this.selectedArray);
        this.props.navigator.pop();
    }
}

ImagePicker.propTypes = propTypes;
ImagePicker.defaultProps = defaultProps;
