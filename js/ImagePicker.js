/**
 * Created by lvbingru on 2/29/16.
 */

import {styles} from './ImagePicker.style.js'

import React, {InteractionManager, Component, PropTypes, View, Text, ListView, Image, Platform, TouchableOpacity, Alert, CameraRoll} from 'react-native';
import Icon from 'react-native-vector-icons/FontAwesome';
import Camera from './Camera';

const propTypes = {
}

const defaultProps = {
    limit : 65535,
}

export default class ImagePicker extends Component {
    constructor(props) {
        super(props);

        this.imagesArray = [];
        this.selectedArray = [];

        this.state = {
            dataSource: new ListView.DataSource({rowHasChanged: (r1, r2) => r1 !== r2 || r1.selected!==r2.selected}).cloneWithRows([]),
        };
    }

    componentDidMount() {
        InteractionManager.runAfterInteractions(() => {
            this.fetchData()
        });
    }

    unSelectedAll() {
        this.selectedArray = []
        this.imagesArray = this.imagesArray.map((el,index)=> {
            if(el.selected) {
                return {...el, selected: false}
            }
            else {
                return el
            }
        })
        this.updateDataSource();
    }

    render() {
        const {limit, onSelectFinished} = this.props.params;
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
                        onPress = {()=>{
                            onSelectFinished && onSelectFinished(this.selectedArray);
                            this.props.navigator.pop();
                        }}
                    >
                        {
                            this.selectedArray.length!==0 &&
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

        const list = this.imagesArray;

        React.CameraRoll.getPhotos({
            first : 100,
            after : (next && list.length > 0)?this.getImage(list[list.length-1]) :undefined,
        }).then(data=>{
            if (data.edges.length) {
                data.edges.forEach((el)=>{
                    this.checkSelected(el)
                    list.push({...el});
                })

                InteractionManager.runAfterInteractions(() => {
                    this.updateDataSource();
                });
            }
        },e=>{
        })
    }

    getImage(obj) {
        return obj.node.image.uri
    }

    checkSelected(obj) {
        const uri = this.getImage(obj)
        for (let i =0;i<this.selectedArray.length;i++) {
            const item = this.selectedArray[i]
            if (uri === this.getImage(item)) {
                obj.selected = true
                this.selectedArray.splice(i,1,obj)
                return
            }
        }
        obj.selected = false
    }

    onSelectedChanged(rowData, rowIndex) {
        const {limit} = this.props.params

        const should = rowData.selected || this.selectedArray.length < limit
        if (!should) {
            Alert.alert("最多还能选择"+limit+"张");
            return false
        }

        if (rowData.selected) {
            const index = this.selectedArray.indexOf(rowData)
            this.selectedArray.splice(index,1)
        }
        else {
            this.selectedArray.push(rowData)
        }
        this.imagesArray = this.imagesArray.map((el,index)=> {
                if (index === rowIndex) {
                    return {...el,selected: !rowData.selected}
                }
                else {
                    return el;
                }
            }
        )
        this.updateDataSource();

        return true
    }

    updateDataSource() {
        const ds = this.imagesArray.slice(4, this.imagesArray.length)
        this.setState({
            dataSource : this.state.dataSource.cloneWithRows(ds),
        })
    }

    onCamera() {
        const {onSelectFinished} = this.props.params;
        Camera.openCameraDialog().then(r=>{
            onSelectFinished && onSelectFinished([r]);
            this.props.navigator.pop();
        });
    }
}

ImagePicker.propTypes = propTypes;
ImagePicker.defaultProps = defaultProps;
